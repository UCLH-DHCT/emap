package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;

/**
 * All interaction with labs tables.
 * @author Stef Piatek
 */
@Component
public class LabController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabNumberRepository labNumberRepo;
    private final LabTestDefinitionRepository labTestDefinitionRepo;
    private final LabBatteryElementRepository labBatteryElementRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;
    private final LabResultRepository labResultRepo;
    private final LabResultAuditRepository labResultAuditRepo;

    public LabController(
            LabBatteryElementRepository labBatteryElementRepo, LabNumberRepository labNumberRepo, LabTestDefinitionRepository labTestDefinitionRepo,
            LabOrderRepository labOrderRepo, LabOrderAuditRepository labOrderAuditRepo, LabResultRepository labResultRepo,
            LabResultAuditRepository labResultAuditRepo) {
        this.labBatteryElementRepo = labBatteryElementRepo;
        this.labNumberRepo = labNumberRepo;
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
    }

    /**
     * @param mrn        MRN
     * @param visit      hospital visit
     * @param msg        order message
     * @param storedFrom time that star started processing the message
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
     * @throws RequiredDataMissingException       if OrderDateTime missing from message
     */
    @Transactional
    public void processLabOrder(Mrn mrn, HospitalVisit visit, LabOrderMsg msg, Instant storedFrom)
            throws IncompatibleDatabaseStateException, RequiredDataMissingException {
        if (msg.getStatusChangeTime() == null) {
            throw new RequiredDataMissingException("LabOrder has no StatusChangeTime in message");
        }

        LabNumber labNumber = getOrCreateLabNumber(mrn, visit, msg, storedFrom);
        Instant validFrom = msg.getStatusChangeTime();
        for (LabResultMsg result : msg.getLabResultMsgs()) {
            LabTestDefinition testDefinition = getOrCreateLabTestDefinition(result, msg, validFrom, storedFrom);
            LabBatteryElement batteryElement = getOrCreateLabBatteryElement(testDefinition, msg, validFrom, storedFrom);
            updateOrCreateLabOrder(batteryElement, labNumber, msg, validFrom, storedFrom);
            RowState<LabResult, LabResultAudit> resultState = updateOrCreateLabResult(labNumber, testDefinition, result, validFrom, storedFrom);
        }
    }

    /**
     * @param mrn        MRN
     * @param visit      hospital visit
     * @param msg        order message
     * @param storedFrom time that star started processing the message
     * @return lab number
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
     */
    private LabNumber getOrCreateLabNumber(
            Mrn mrn, HospitalVisit visit, LabOrderMsg msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        LabNumber labNumber = labNumberRepo
                .findByMrnIdAndHospitalVisitIdAndInternalLabNumberAndExternalLabNumber(
                        mrn, visit, msg.getLabSpecimenNumber(), msg.getEpicCareOrderNumber())
                .orElseGet(() -> {
                    LabNumber labNum = new LabNumber(
                            mrn, visit, msg.getLabSpecimenNumber(), msg.getEpicCareOrderNumber(),
                            msg.getSpecimenType(), msg.getSourceSystem(), storedFrom);
                    logger.trace("Creating new lab number {}", labNum);
                    return labNumberRepo.save(labNum);
                });

        if (!labNumber.getSpecimenType().equals(msg.getSpecimenType())) {
            throw new IncompatibleDatabaseStateException("Message specimen type doesn't match the database");
        }
        return labNumber;
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

    private void updateOrCreateLabOrder(
            LabBatteryElement batteryElement, LabNumber labNumber, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabOrder, LabOrderAudit> orderState = labOrderRepo
                .findByLabBatteryElementIdAndLabNumberId(batteryElement, labNumber)
                .map(order -> new RowState<>(order, validFrom, storedFrom, false))
                .orElseGet(() -> createLabOrder(batteryElement, labNumber, validFrom, storedFrom));
        updateLabOrder(orderState, msg);
    }

    private RowState<LabOrder, LabOrderAudit> createLabOrder(LabBatteryElement batteryElement, LabNumber labNumber, Instant validFrom, Instant storedFrom) {
        LabOrder order = new LabOrder(batteryElement, labNumber);
        order.setValidFrom(validFrom);
        order.setStoredFrom(storedFrom);
        return new RowState<>(order, validFrom, storedFrom, false);
    }

    private void updateLabOrder(RowState<LabOrder, LabOrderAudit> orderState, LabOrderMsg msg) {
        // update all lab order time information
        orderState.saveEntityOrAuditLogIfRequired(labOrderRepo, labOrderAuditRepo);
    }


    private RowState<LabResult, LabResultAudit> updateOrCreateLabResult(
            LabNumber labNumber, LabTestDefinition testDefinition, LabResultMsg result, Instant validFrom, Instant storedFrom) {
        RowState<LabResult, LabResultAudit> resultState = labResultRepo
                .findByLabNumberIdAndLabTestDefinitionId(labNumber, testDefinition)
                .map(r -> new RowState<>(r, result.getResultTime(), storedFrom, false))
                .orElseGet(() -> createLabResult(labNumber, testDefinition, result.getResultTime(), validFrom, storedFrom));

        if (!resultState.isEntityCreated() && result.getResultTime().isBefore(resultState.getEntity().getResultLastModifiedTime())) {
            logger.trace("LabResult database is more recent than LabResult message, not doing anything");
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
            resultState.assignInterchangeValue(resultMsg.getNumericValue(), labResult.getResultAsReal(), labResult::setResultAsReal);
            resultState.assignIfDifferent(resultMsg.getResultOperator(), labResult.getResultOperator(), labResult::setResultOperator);
        } else {
            resultState.assignIfDifferent(resultMsg.getStringValue(), labResult.getResultAsText(), labResult::setResultAsText);
        }

        resultState.assignInterchangeValue(resultMsg.getUnits(), labResult.getUnits(), labResult::setUnits);
        resultState.assignInterchangeValue(resultMsg.getReferenceLow(), labResult.getRangeLow(), labResult::setRangeLow);
        resultState.assignInterchangeValue(resultMsg.getReferenceHigh(), labResult.getRangeHigh(), labResult::setRangeHigh);
        resultState.assignIfDifferent(resultMsg.isAbnormal(), labResult.getAbnormal(), labResult::setAbnormal);
        resultState.assignInterchangeValue(resultMsg.getNotes(), labResult.getComment(), labResult::setComment);
        resultState.assignIfDifferent(resultMsg.getResultStatus(), labResult.getResultStatus(), labResult::setResultStatus);

        if (resultState.isEntityUpdated()) {
            labResult.setResultLastModifiedTime(resultMsg.getResultTime());
        }
    }
}
