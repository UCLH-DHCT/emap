package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabIsolateAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabIsolateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSensitivityAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSensitivityRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolate;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolateAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivity;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivityAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;

/**
 * Controller for LabResult specific information.
 * @author Stef Piatek
 */
@Component
class LabResultController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabResultRepository labResultRepo;
    private final LabResultAuditRepository labResultAuditRepo;
    private final LabIsolateRepository labIsolateRepo;
    private final LabIsolateAuditRepository labIsolateAuditRepo;
    private final LabSensitivityRepository labSensitivityRepo;
    private final LabSensitivityAuditRepository labSensitivityAuditRepo;

    LabResultController(
            LabResultRepository labResultRepo, LabResultAuditRepository labResultAuditRepo,
            LabIsolateRepository labIsolateRepo, LabIsolateAuditRepository labIsolateAuditRepo,
            LabSensitivityRepository labResultSensitivityRepo, LabSensitivityAuditRepository labSensitivityAuditRepo
    ) {
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
        this.labIsolateRepo = labIsolateRepo;
        this.labIsolateAuditRepo = labIsolateAuditRepo;
        this.labSensitivityRepo = labResultSensitivityRepo;
        this.labSensitivityAuditRepo = labSensitivityAuditRepo;
    }

    @Transactional
    public void processResult(LabTestDefinition testDefinition, LabOrder labOrder, LabResultMsg resultMsg, Instant validFrom, Instant storedFrom) {
        RowState<LabResult, LabResultAudit> labResultState = updateOrCreateLabResult(labOrder, testDefinition, resultMsg, validFrom, storedFrom);
        // If lab isolate, update or create them
        LabIsolateMsg isolateMsg = resultMsg.getLabIsolate();
        if (isolateMsg != null && !validFrom.isBefore(labResultState.getEntity().getResultLastModifiedTime())) {
            LabIsolate isolate = updateOrCreateIsolateAndUpdateLabResult(labResultState, isolateMsg, validFrom, storedFrom);
            for (LabResultMsg sensResult : isolateMsg.getSensitivities()) {
                updateOrCreateSensitivity(isolate, sensResult, validFrom, storedFrom);
            }
        }
    }

    /**
     * Updates or creates lab result.
     * <p>
     * Special processing for microbiology isolates:
     * valueAsText stores the isolate name^text (can also be no growth like NG2^No growth after 2 days)
     * units stores the CFU for an isolate, culturing method for no growth
     * comment stores the clinical notes for the isolate
     * @param labOrder       lab order
     * @param testDefinition test definition
     * @param result         lab result msg
     * @param validFrom      most recent change to results
     * @param storedFrom     time that star encountered the message
     * @return lab result wrapped in row state
     */
    private RowState<LabResult, LabResultAudit> updateOrCreateLabResult(
            LabOrder labOrder, LabTestDefinition testDefinition, LabResultMsg result, Instant validFrom, Instant storedFrom) {
        RowState<LabResult, LabResultAudit> resultState;
        resultState = labResultRepo
                .findByLabOrderIdAndLabTestDefinitionId(labOrder, testDefinition)
                .map(r -> new RowState<>(r, result.getResultTime(), storedFrom, false))
                .orElseGet(() -> createLabResult(labOrder, testDefinition, result.getResultTime(), validFrom, storedFrom));

        if (!resultState.isEntityCreated() && result.getResultTime().isBefore(resultState.getEntity().getResultLastModifiedTime())) {
            logger.trace("LabResult database is more recent than LabResult message, not updating information");
            return resultState;
        }

        updateLabResult(resultState, result);

        resultState.saveEntityOrAuditLogIfRequired(labResultRepo, labResultAuditRepo);
        return resultState;
    }

    private RowState<LabResult, LabResultAudit> createLabResult(
            LabOrder labOrder, LabTestDefinition testDefinition, Instant resultModified, Instant validFrom, Instant storedFrom) {
        LabResult labResult = new LabResult(labOrder, testDefinition, resultModified);
        return new RowState<>(labResult, validFrom, storedFrom, true);
    }

    private void updateLabResult(RowState<LabResult, LabResultAudit> resultState, LabResultMsg resultMsg) {
        LabResult labResult = resultState.getEntity();
        resultState.assignInterchangeValue(resultMsg.getUnits(), labResult.getUnits(), labResult::setUnits);
        resultState.assignInterchangeValue(resultMsg.getReferenceLow(), labResult.getRangeLow(), labResult::setRangeLow);
        resultState.assignInterchangeValue(resultMsg.getReferenceHigh(), labResult.getRangeHigh(), labResult::setRangeHigh);
        resultState.assignInterchangeValue(resultMsg.getAbnormalFlag(), labResult.getAbnormalFlag(), labResult::setAbnormalFlag);
        resultState.assignInterchangeValue(resultMsg.getNotes(), labResult.getComment(), labResult::setComment);
        resultState.assignIfDifferent(resultMsg.getResultStatus(), labResult.getResultStatus(), labResult::setResultStatus);
        resultState.assignIfDifferent(resultMsg.getMimeType().toString(), labResult.getMimeType(), labResult::setMimeType);
        if (ValueType.NUMERIC == resultMsg.getMimeType()) {
            resultState.assignInterchangeValue(resultMsg.getNumericValue(), labResult.getValueAsReal(), labResult::setValueAsReal);
            resultState.assignIfDifferent(resultMsg.getResultOperator(), labResult.getResultOperator(), labResult::setResultOperator);
        } else if (ValueType.TEXT == resultMsg.getMimeType()) {
            resultState.assignInterchangeValue(resultMsg.getStringValue(), labResult.getValueAsText(), labResult::setValueAsText);
        }

        if (resultState.isEntityUpdated()) {
            labResult.setResultLastModifiedTime(resultMsg.getResultTime());
        }
    }


    /**
     * Update or creat isolate, if isolate is changed then also update it's lab result's modified time.
     * @param labResultState result state
     * @param isolateMsg     isolate interchange message
     * @param validFrom      most recent change to results
     * @param storedFrom     time that star encountered the message
     * @return lab isolate entity
     */
    private LabIsolate updateOrCreateIsolateAndUpdateLabResult(
            RowState<LabResult, LabResultAudit> labResultState, LabIsolateMsg isolateMsg, Instant validFrom, Instant storedFrom) {
        LabResult labResult = labResultState.getEntity();
        RowState<LabIsolate, LabIsolateAudit> isolateState = labIsolateRepo
                .findByLabResultIdAndLabInternalId(labResult, isolateMsg.getIsolateId())
                .map(isolate -> new RowState<>(isolate, validFrom, storedFrom, false))
                .orElseGet(() -> createLabIsolate(labResult, isolateMsg.getIsolateId(), validFrom, storedFrom));
        LabIsolate labIsolate = isolateState.getEntity();

        if (isolateState.isEntityCreated() || validFrom.isAfter(labIsolate.getValidFrom())) {
            isolateState.assignIfDifferent(isolateMsg.getIsolateCode(), labIsolate.getIsolateCode(), labIsolate::setIsolateCode);
            isolateState.assignIfDifferent(isolateMsg.getIsolateName(), labIsolate.getIsolateName(), labIsolate::setIsolateName);
            isolateState.assignInterchangeValue(isolateMsg.getCultureType(), labIsolate.getCultureType(), labIsolate::setCultureType);
            isolateState.assignInterchangeValue(isolateMsg.getQuantity(), labIsolate.getQuantity(), labIsolate::setQuantity);
            isolateState.assignInterchangeValue(
                    isolateMsg.getClinicalInformation(), labIsolate.getClinicalInformation(), labIsolate::setClinicalInformation);
        }

        // if change in isolate state we should update the time of the result
        // because the result is a link, and this has changed
        if (isolateState.isEntityUpdated()) {
            labResultState.assignIfDifferent(validFrom, labResult.getResultLastModifiedTime(), labResult::setResultLastModifiedTime);
        }

        isolateState.saveEntityOrAuditLogIfRequired(labIsolateRepo, labIsolateAuditRepo);
        labResultState.saveEntityOrAuditLogIfRequired(labResultRepo, labResultAuditRepo);
        return labIsolate;
    }

    private RowState<LabIsolate, LabIsolateAudit> createLabIsolate(LabResult labResult, String isolateCode, Instant validFrom, Instant storedFrom) {
        LabIsolate isolate = new LabIsolate(labResult, isolateCode);
        return new RowState<>(isolate, validFrom, storedFrom, true);

    }

    private void updateOrCreateSensitivity(LabIsolate isolate, LabResultMsg sensitivityMsg, Instant validFrom, Instant storedFrom) {
        if (sensitivityMsg.getStringValue().isUnknown()) {
            return;
        }
        RowState<LabSensitivity, LabSensitivityAudit> sensitivityState = labSensitivityRepo
                .findByLabIsolateIdAndAgent(isolate, sensitivityMsg.getStringValue().get())
                .map(sens -> new RowState<>(sens, validFrom, storedFrom, false))
                .orElseGet(() -> createSensitivity(isolate, sensitivityMsg.getStringValue().get(), validFrom, storedFrom));

        LabSensitivity sensitivity = sensitivityState.getEntity();

        if (sensitivityState.isEntityCreated() || validFrom.isAfter(sensitivity.getReportingDatetime())) {
            sensitivityState.assignInterchangeValue(sensitivityMsg.getAbnormalFlag(), sensitivity.getSensitivity(), sensitivity::setSensitivity);
            if (sensitivityState.isEntityUpdated()) {
                sensitivityState.assignIfDifferent(validFrom, sensitivity.getReportingDatetime(), sensitivity::setReportingDatetime);
            }
            sensitivityState.saveEntityOrAuditLogIfRequired(labSensitivityRepo, labSensitivityAuditRepo);
        }
    }

    private RowState<LabSensitivity, LabSensitivityAudit> createSensitivity(
            LabIsolate labIsolate, String agent, Instant validFrom, Instant storedFrom) {
        LabSensitivity sensitivity = new LabSensitivity(labIsolate, agent);
        sensitivity.setReportingDatetime(validFrom);
        return new RowState<>(sensitivity, validFrom, storedFrom, true);
    }

}
