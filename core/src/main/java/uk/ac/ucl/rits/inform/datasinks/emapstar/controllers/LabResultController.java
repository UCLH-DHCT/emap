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
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Controller for LabResult specific information.
 *
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

    LabResultController(
            LabTestDefinitionRepository labTestDefinitionRepo, LabBatteryElementRepository labBatteryElementRepo, LabOrderRepository labOrderRepo,
            LabOrderAuditRepository labOrderAuditRepo, LabResultRepository labResultRepo, LabResultAuditRepository labResultAuditRepo) {
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labBatteryElementRepo = labBatteryElementRepo;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
    }

    @Transactional
    public void processResult(LabOrderMsg msg, LabNumber labNumber, LabResultMsg result, Instant validFrom, Instant storedFrom) {
        LabTestDefinition testDefinition = getOrCreateLabTestDefinition(result, msg, validFrom, storedFrom);
        LabBatteryElement batteryElement = getOrCreateLabBatteryElement(testDefinition, msg, validFrom, storedFrom);
        RowState<LabOrder, LabOrderAudit> orderState = updateOrCreateLabOrder(batteryElement, labNumber, msg, validFrom, storedFrom);
        RowState<LabResult, LabResultAudit> resultState = updateOrCreateLabResult(labNumber, testDefinition, result, validFrom, storedFrom);
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
     * @return Lab order wrapped in row state
     */
    private RowState<LabOrder, LabOrderAudit> updateOrCreateLabOrder(
            LabBatteryElement batteryElement, LabNumber labNumber, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabOrder, LabOrderAudit> orderState = labOrderRepo
                .findByLabBatteryElementIdAndLabNumberId(batteryElement, labNumber)
                .map(order -> new RowState<>(order, validFrom, storedFrom, false))
                .orElseGet(() -> createLabOrder(batteryElement, labNumber, validFrom, storedFrom));

        updateLabOrder(orderState, msg, validFrom);
        orderState.saveEntityOrAuditLogIfRequired(labOrderRepo, labOrderAuditRepo);
        return orderState;
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

    private RowState<LabResult, LabResultAudit> updateOrCreateLabResult(
            LabNumber labNumber, LabTestDefinition testDefinition, LabResultMsg result, Instant validFrom, Instant storedFrom) {
        RowState<LabResult, LabResultAudit> resultState = labResultRepo
                .findByLabNumberIdAndLabTestDefinitionId(labNumber, testDefinition)
                .map(r -> new RowState<>(r, result.getResultTime(), storedFrom, false))
                .orElseGet(() -> createLabResult(labNumber, testDefinition, result.getResultTime(), validFrom, storedFrom));

        if (!resultState.isEntityCreated() && result.getResultTime().isBefore(resultState.getEntity().getResultLastModifiedTime())) {
            logger.trace("LabResult database is more recent than LabResult message, not updating information");
            return resultState;
        }

        updateLabResult(resultState, result);

        resultState.saveEntityOrAuditLogIfRequired(labResultRepo, labResultAuditRepo);
        return resultState;
    }

    private RowState<LabResult, LabResultAudit> createLabResult(
            LabNumber labNumber, LabTestDefinition testDefinition, Instant resultModified, Instant validFrom, Instant storedFrom) {
        LabResult labResult = new LabResult(labNumber, testDefinition, resultModified);
        return new RowState<>(labResult, validFrom, storedFrom, true);
    }

    private void updateLabResult(RowState<LabResult, LabResultAudit> resultState, LabResultMsg resultMsg) {
        LabResult labResult = resultState.getEntity();
        if (resultMsg.isNumeric()) {
            resultState.assignInterchangeValue(resultMsg.getNumericValue(), labResult.getValueAsReal(), labResult::setValueAsReal);
            resultState.assignIfDifferent(resultMsg.getResultOperator(), labResult.getResultOperator(), labResult::setResultOperator);
        } else {
            resultState.assignInterchangeValue(resultMsg.getStringValue(), labResult.getValueAsText(), labResult::setValueAsText);
        }

        resultState.assignInterchangeValue(resultMsg.getUnits(), labResult.getUnits(), labResult::setUnits);
        resultState.assignInterchangeValue(resultMsg.getReferenceLow(), labResult.getRangeLow(), labResult::setRangeLow);
        resultState.assignInterchangeValue(resultMsg.getReferenceHigh(), labResult.getRangeHigh(), labResult::setRangeHigh);
        resultState.assignInterchangeValue(resultMsg.getAbnormalFlag(), labResult.getAbnormalFlag(), labResult::setAbnormalFlag);
        resultState.assignInterchangeValue(resultMsg.getNotes(), labResult.getComment(), labResult::setComment);
        resultState.assignIfDifferent(resultMsg.getResultStatus(), labResult.getResultStatus(), labResult::setResultStatus);

        if (resultState.isEntityUpdated()) {
            labResult.setResultLastModifiedTime(resultMsg.getResultTime());
        }
    }

}
