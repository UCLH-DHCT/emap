package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;

/**
 * Main class that interacts with labs tables, either directly or through sub controllers.
 * @author Stef Piatek
 */
@Component
public class LabController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabOrderController labOrderController;
    private final LabTestDefinitionRepository labTestDefinitionRepo;
    private final LabBatteryElementRepository labBatteryElementRepo;
    private final LabResultController labResultController;

    public LabController(
            LabOrderController labOrderController, LabTestDefinitionRepository labTestDefinitionRepo,
            LabBatteryElementRepository labBatteryElementRepo, LabResultController labResultController) {
        this.labOrderController = labOrderController;
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labBatteryElementRepo = labBatteryElementRepo;
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
        LabBattery battery = labOrderController.getOrCreateLabBattery(msg, validFrom, storedFrom);
        LabNumber labNumber = labOrderController.processLabNumberLabCollectionAndLabOrder(mrn, visit, battery, msg, validFrom, storedFrom);
        for (LabResultMsg result : msg.getLabResultMsgs()) {
            LabTestDefinition testDefinition = getOrCreateLabTestDefinition(result, msg, storedFrom, validFrom);
            getOrCreateLabBatteryElement(testDefinition, battery, storedFrom, validFrom);
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

    private void getOrCreateLabBatteryElement(LabTestDefinition testDefinition, LabBattery battery, Instant validFrom, Instant storedFrom) {
        labBatteryElementRepo
                .findByLabBatteryIdAndLabTestDefinitionId(battery, testDefinition)
                .orElseGet(() -> {
                    LabBatteryElement batteryElement = new LabBatteryElement(testDefinition, battery, validFrom, storedFrom);
                    logger.trace("Creating new Lab Test Battery Element {}", batteryElement);
                    return labBatteryElementRepo.save(batteryElement);
                });
    }

}
