package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageCancelledException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import javax.annotation.Resource;
import java.time.Instant;

/**
 * Main class that interacts with labs tables, either directly or through sub controllers.
 * @author Stef Piatek
 */
@Component
public class LabController {
    /**
     * Self-autowire so that @Caching annotation call will be intercepted.
     * Spring does not intercept internal calls, so using self here means that it will be intercepted for caching.
     */
    @Resource
    private LabController self;

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
     * @throws RequiredDataMissingException       if OrderDateTime missing from message or mime type is unrecognised
     * @throws MessageCancelledException          Lab Order was previously cancelled
     */
    @Transactional
    public void processLabOrder(Mrn mrn, @Nullable HospitalVisit visit, LabOrderMsg msg, Instant storedFrom)
            throws IncompatibleDatabaseStateException, RequiredDataMissingException, MessageCancelledException {
        if (msg.getStatusChangeTime() == null) {
            throw new RequiredDataMissingException("LabOrder has no StatusChangeTime in message");
        }
        Instant validFrom = msg.getStatusChangeTime();
        LabBattery battery = labOrderController.getOrCreateLabBattery(
                msg.getTestBatteryLocalCode(), msg.getTestBatteryCodingSystem(), validFrom, storedFrom);
        if (msg.getEpicCareOrderNumber().isDelete()) {
            labOrderController.processLabSampleAndDeleteLabOrder(mrn, battery, visit, msg, validFrom, storedFrom);
            return;
        }

        LabOrder labOrder = labOrderController.processSampleAndOrderInformation(mrn, visit, battery, msg, validFrom, storedFrom);
        for (LabResultMsg result : msg.getLabResultMsgs()) {
            LabTestDefinition testDefinition = self.getOrCreateLabTestDefinition(
                    msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode(), storedFrom, validFrom);
            getOrCreateLabBatteryElement(testDefinition, battery, storedFrom, validFrom);
            labResultController.processResult(testDefinition, labOrder, result, validFrom, storedFrom);
        }
    }


    /**
     * @param testLabCode   local result item code
     * @param labProvider   battery coding system
     * @param labDepartment lab department
     * @param validFrom     most recent change to results
     * @param storedFrom    time that star started processing the message
     * @return LabTestDefinition entity
     */
    @Cacheable(value = "labTestDefinition", key = "{ #labProvider , #labDepartment, #testLabCode }")
    public LabTestDefinition getOrCreateLabTestDefinition(
            String labProvider, String labDepartment, String testLabCode, Instant validFrom, Instant storedFrom) {
        return labTestDefinitionRepo
                .findByLabProviderAndLabDepartmentAndTestLabCode(labProvider, labDepartment, testLabCode)
                .orElseGet(() -> {
                    LabTestDefinition testDefinition = new LabTestDefinition(labProvider, labDepartment, testLabCode);
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
