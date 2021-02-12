package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultSensitivityAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultSensitivityRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivity;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivityAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
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
    private final LabResultSensitivityRepository labResultSensitivityRepo;
    private final LabResultSensitivityAuditRepository labResultSensitivityAuditRepo;

    LabResultController(
            LabResultRepository labResultRepo, LabResultAuditRepository labResultAuditRepo,
            LabResultSensitivityRepository labResultSensitivityRepo, LabResultSensitivityAuditRepository labResultSensitivityAuditRepo) {
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
        this.labResultSensitivityRepo = labResultSensitivityRepo;
        this.labResultSensitivityAuditRepo = labResultSensitivityAuditRepo;
    }

    @Transactional
    public void processResult(LabTestDefinition testDefinition, LabNumber labNumber, LabResultMsg resultMsg, Instant validFrom, Instant storedFrom) {
        LabResult labResult = updateOrCreateLabResult(labNumber, testDefinition, resultMsg, validFrom, storedFrom);
        // If any lab sensitivities, update or create them
        for (LabOrderMsg sensOrder : resultMsg.getLabSensitivities()) {
            for (LabResultMsg sensResult : sensOrder.getLabResultMsgs()) {
                updateOrCreateSensitivity(labResult, sensResult, validFrom, storedFrom);
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
     * @param labNumber      lab number
     * @param testDefinition test definition
     * @param result         lab result msg
     * @param validFrom      most recent change to results
     * @param storedFrom     time that star encountered the message
     * @return lab result wrapped in row state
     */
    private LabResult updateOrCreateLabResult(
            LabNumber labNumber, LabTestDefinition testDefinition, LabResultMsg result, Instant validFrom, Instant storedFrom) {
        RowState<LabResult, LabResultAudit> resultState;
        if (result.getIsolateCodeAndText().isEmpty()) {
            resultState = labResultRepo
                    .findByLabNumberIdAndLabTestDefinitionId(labNumber, testDefinition)
                    .map(r -> new RowState<>(r, result.getResultTime(), storedFrom, false))
                    .orElseGet(() -> createLabResult(labNumber, testDefinition, result.getResultTime(), validFrom, storedFrom));
        } else {
            // multiple isolates in a result, so these don't get overwritten with different isolates
            // get by the isolate code as well as the lab number and test definition
            resultState = labResultRepo
                    .findByLabNumberIdAndLabTestDefinitionIdAndValueAsText(labNumber, testDefinition, result.getIsolateCodeAndText())
                    .map(r -> new RowState<>(r, result.getResultTime(), storedFrom, false))
                    .orElseGet(() -> createLabResult(labNumber, testDefinition, result.getResultTime(), validFrom, storedFrom));
        }

        if (!resultState.isEntityCreated() && result.getResultTime().isBefore(resultState.getEntity().getResultLastModifiedTime())) {
            logger.trace("LabResult database is more recent than LabResult message, not updating information");
            return resultState.getEntity();
        }

        updateLabResult(resultState, result);

        resultState.saveEntityOrAuditLogIfRequired(labResultRepo, labResultAuditRepo);
        return resultState.getEntity();
    }

    private RowState<LabResult, LabResultAudit> createLabResult(
            LabNumber labNumber, LabTestDefinition testDefinition, Instant resultModified, Instant validFrom, Instant storedFrom) {
        LabResult labResult = new LabResult(labNumber, testDefinition, resultModified);
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

        if (resultMsg.isNumeric()) {
            resultState.assignInterchangeValue(resultMsg.getNumericValue(), labResult.getValueAsReal(), labResult::setValueAsReal);
            resultState.assignIfDifferent(resultMsg.getResultOperator(), labResult.getResultOperator(), labResult::setResultOperator);
        } else if (!resultMsg.getIsolateCodeAndText().isEmpty()) {
            // Sadly some custom use of fields for Isolates:
            // result -  have no isolate detected or isolate type
            resultState.assignIfDifferent(resultMsg.getIsolateCodeAndText(), labResult.getValueAsText(), labResult::setValueAsText);
            // unit -  CFU (if present)
            resultState.assignInterchangeValue(resultMsg.getStringValue(), labResult.getUnits(), labResult::setUnits);
            // comment - lab sensitivity clinical notes
            InterchangeValue<String> clinicalInfo = resultMsg.getLabSensitivities().stream()
                    .map(LabOrderMsg::getClinicalInformation)
                    .findFirst()
                    .orElseGet(InterchangeValue::unknown);
            resultState.assignInterchangeValue(clinicalInfo, labResult.getComment(), labResult::setComment);
        } else {
            resultState.assignInterchangeValue(resultMsg.getStringValue(), labResult.getValueAsText(), labResult::setValueAsText);
        }

        if (resultState.isEntityUpdated()) {
            labResult.setResultLastModifiedTime(resultMsg.getResultTime());
        }
    }

    private void updateOrCreateSensitivity(LabResult labResult, LabResultMsg sensitivityMsg, Instant validFrom, Instant storedFrom) {
        if (sensitivityMsg.getStringValue().isUnknown()) {
            return;
        }
        RowState<LabResultSensitivity, LabResultSensitivityAudit> sensitivityState = labResultSensitivityRepo
                .findByLabResultIdAndAgent(labResult, sensitivityMsg.getStringValue().get())
                .map(sens -> new RowState<>(sens, validFrom, storedFrom, false))
                .orElseGet(() -> createSensitivity(labResult, sensitivityMsg.getStringValue().get(), validFrom, storedFrom));

        LabResultSensitivity sensitivity = sensitivityState.getEntity();
        sensitivityState.assignInterchangeValue(sensitivityMsg.getAbnormalFlag(), sensitivity.getSensitivity(), sensitivity::setSensitivity);
        if (sensitivityState.isEntityUpdated()) {
            sensitivityState.assignIfDifferent(validFrom, sensitivity.getReportingDatetime(), sensitivity::setReportingDatetime);
        }
        sensitivityState.saveEntityOrAuditLogIfRequired(labResultSensitivityRepo, labResultSensitivityAuditRepo);
    }

    private RowState<LabResultSensitivity, LabResultSensitivityAudit> createSensitivity(
            LabResult labResult, String agent, Instant validFrom, Instant storedFrom) {
        LabResultSensitivity sensitivity = new LabResultSensitivity(labResult, agent);
        sensitivity.setReportingDatetime(validFrom);
        return new RowState<>(sensitivity, validFrom, storedFrom, true);
    }

}
