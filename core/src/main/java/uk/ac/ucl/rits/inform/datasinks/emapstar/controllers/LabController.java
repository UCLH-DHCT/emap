package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Main class that interacts with labs tables, either directly or through sub controllers.
 * @author Stef Piatek
 */
@Component
public class LabController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabNumberController labNumberController;
    private final LabTestDefinitionRepository labTestDefinitionRepo;
    private final LabBatteryElementRepository labBatteryElementRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;
    private final LabResultController labResultController;

    public LabController(
            LabNumberController labNumberController, LabTestDefinitionRepository labTestDefinitionRepo,
            LabBatteryElementRepository labBatteryElementRepo, LabOrderRepository labOrderRepo, LabOrderAuditRepository labOrderAuditRepo,
            LabResultController labResultController) {
        this.labNumberController = labNumberController;
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labBatteryElementRepo = labBatteryElementRepo;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.labResultController = labResultController;
    }

    /**
     * @param mrn        MRN
     * @param visit      hospital visit, can be null
     * @param msg        order message
     * @param storedFrom time that star started processing the message
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
     * @throws RequiredDataMissingException       if OrderDateTime missing from message
     */
    @Transactional
    public void processLabOrder(Mrn mrn, @Nullable HospitalVisit visit, LabOrderMsg msg, Instant storedFrom)
            throws IncompatibleDatabaseStateException, RequiredDataMissingException {
        if (msg.getStatusChangeTime() == null) {
            throw new RequiredDataMissingException("LabOrder has no StatusChangeTime in message");
        }
        Instant validFrom = msg.getStatusChangeTime();
        LabNumber labNumber = labNumberController.processLabNumberAndLabCollection(mrn, visit, msg, validFrom, storedFrom);
        for (LabResultMsg result : msg.getLabResultMsgs()) {
            LabTestDefinition testDefinition = getOrCreateLabTestDefinition(result, msg, storedFrom, validFrom);
            LabBatteryElement batteryElement = getOrCreateLabBatteryElement(testDefinition, msg, storedFrom, validFrom);
            updateOrCreateLabOrder(batteryElement, labNumber, msg, validFrom, storedFrom);
            labResultController.processResult(testDefinition, labNumber, result, validFrom, storedFrom);
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
}
