package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultSensitivityAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultSensitivityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivity;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivityAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Controller for LabResult specific information.
 * @author Stef Piatek
 */
@Component
class LabResultController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabTestDefinitionRepository labTestDefinitionRepo;
    private final LabBatteryElementRepository labBatteryElementRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;
    private final LabResultRepository labResultRepo;
    private final LabResultAuditRepository labResultAuditRepo;
    private final LabResultSensitivityRepository labResultSensitivityRepo;
    private final LabResultSensitivityAuditRepository labResultSensitivityAuditRepo;

    LabResultController(
            LabTestDefinitionRepository labTestDefinitionRepo, LabBatteryElementRepository labBatteryElementRepo, LabOrderRepository labOrderRepo,
            LabOrderAuditRepository labOrderAuditRepo, LabResultRepository labResultRepo, LabResultAuditRepository labResultAuditRepo,
            LabResultSensitivityRepository labResultSensitivityRepo, LabResultSensitivityAuditRepository labResultSensitivityAuditRepo) {
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labBatteryElementRepo = labBatteryElementRepo;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
        this.labResultSensitivityRepo = labResultSensitivityRepo;
        this.labResultSensitivityAuditRepo = labResultSensitivityAuditRepo;
    }

    @Transactional
    public void processResult(LabOrderMsg msg, LabNumber labNumber, LabResultMsg resultMsg, Instant validFrom, Instant storedFrom) {
        LabTestDefinition testDefinition = getOrCreateLabTestDefinition(resultMsg, msg, validFrom, storedFrom);
        LabResult labResult = updateOrCreateLabResult(labNumber, testDefinition, resultMsg, validFrom, storedFrom);
        LabBatteryElement batteryElement = getOrCreateLabBatteryElement(testDefinition, msg, validFrom, storedFrom);
        updateOrCreateLabOrder(batteryElement, labNumber, msg, validFrom, storedFrom);
        // If any lab sensitivities, update or create them
        for (LabOrderMsg sensOrder : resultMsg.getLabSensitivities()) {
            for (LabResultMsg sensResult : sensOrder.getLabResultMsgs()) {
                updateOrCreateSensitivity(labResult, sensResult, validFrom, storedFrom);
            }
        }
    }

    private LabTestDefinition getOrCreateLabTestDefinition(LabResultMsg result, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        return labTestDefinitionRepo
                .findByLabProviderAndLabDepartmentAndTestLabCode(
                        msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode())
                .orElseGet(() -> {
                    LabTestDefinition testDefinition = new LabTestDefinition(
                            msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode());
                    testDefinition.setValidFrom(validFrom);
                    testDefinition.setStoredFrom(storedFrom);
                    logger.trace("Creating new Lab Test Definition {}", testDefinition);
                    return labTestDefinitionRepo.save(testDefinition);
                });
    }

    private LabBatteryElement getOrCreateLabBatteryElement(LabTestDefinition testDefinition, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        return labBatteryElementRepo
                .findByBatteryAndLabTestDefinitionIdAndLabProvider(
                        msg.getTestBatteryLocalCode(), testDefinition, msg.getTestBatteryCodingSystem())
                .orElseGet(() -> {
                    LabBatteryElement batteryElement = new LabBatteryElement(
                            testDefinition, msg.getTestBatteryLocalCode(), msg.getTestBatteryCodingSystem());
                    batteryElement.setValidFrom(validFrom);
                    batteryElement.setStoredFrom(storedFrom);
                    logger.trace("Creating new Lab Test Battery Element {}", batteryElement);
                    return labBatteryElementRepo.save(batteryElement);
                });
    }

    /**
     * Create new lab order or update existing one.
     * As temporal data can come from different sources, update each one if it is currently null, or the message is newer and has a different value.
     * @param batteryElement Lab Battery Element
     * @param labNumber      LabNumber
     * @param msg            Msg
     * @param validFrom      most recent change to results
     * @param storedFrom     time that star encountered the message
     */
    private void updateOrCreateLabOrder(
            LabBatteryElement batteryElement, LabNumber labNumber, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabOrder, LabOrderAudit> orderState = labOrderRepo
                .findByLabBatteryElementIdAndLabNumberId(batteryElement, labNumber)
                .map(order -> new RowState<>(order, validFrom, storedFrom, false))
                .orElseGet(() -> createLabOrder(batteryElement, labNumber, validFrom, storedFrom));

        updateLabOrder(orderState, msg, validFrom);
        orderState.saveEntityOrAuditLogIfRequired(labOrderRepo, labOrderAuditRepo);
    }

    private RowState<LabOrder, LabOrderAudit> createLabOrder(
            LabBatteryElement batteryElement, LabNumber labNumber, Instant validFrom, Instant storedFrom) {
        LabOrder order = new LabOrder(batteryElement, labNumber);
        order.setValidFrom(validFrom);
        order.setStoredFrom(storedFrom);
        return new RowState<>(order, validFrom, storedFrom, true);
    }

    private void updateLabOrder(RowState<LabOrder, LabOrderAudit> orderState, LabOrderMsg msg, Instant validFrom) {
        LabOrder order = orderState.getEntity();
        orderState.assignInterchangeValue(msg.getClinicalInformation(), order.getClinicalInformation(), order::setClinicalInformation);
        assignIfCurrentlyNullOrNewerAndDifferent(
                orderState, msg.getOrderDateTime(), order.getOrderDatetime(), order::setOrderDatetime, validFrom);
        assignIfCurrentlyNullOrNewerAndDifferent(
                orderState, msg.getRequestedDateTime(), order.getRequestDatetime(), order::setRequestDatetime, validFrom);
        assignIfCurrentlyNullOrNewerAndDifferent(
                orderState, msg.getSampleReceivedTime(), order.getSampleDatetime(), order::setSampleDatetime, validFrom);
    }

    private void assignIfCurrentlyNullOrNewerAndDifferent(
            RowState<?, ?> state, InterchangeValue<Instant> msgValue, Instant currentValue, Consumer<Instant> setter, Instant validFrom) {
        if (currentValue == null || validFrom.isAfter(state.getEntity().getValidFrom())) {
            state.assignInterchangeValue(msgValue, currentValue, setter);
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
        if ( sensitivityState.isEntityUpdated()) {
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
