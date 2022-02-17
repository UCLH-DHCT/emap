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

    @Resource
    private LabCache cache;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabOrderController labOrderController;
    private final LabResultController labResultController;

    /**
     * @param labOrderController  controller for LabOrder tables
     * @param labResultController controller for LabResult tables
     */
    public LabController(
            LabOrderController labOrderController, LabResultController labResultController) {
        this.labOrderController = labOrderController;
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
            logger.trace("** Starting to process lab result {} from {}", result.getTestItemLocalCode(), msg.getTestBatteryCodingSystem());
            LabTestDefinition testDefinition = cache.getOrCreateLabTestDefinition(
                    msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode(), storedFrom, validFrom);
            cache.createLabBatteryElementIfNotExists(testDefinition, battery, storedFrom, validFrom);
            labResultController.processResult(testDefinition, labOrder, result, validFrom, storedFrom);
        }
    }

}

/**
 * Helper component, used because Spring cache doesn't intercept self-invoked method calls.
 */
@Component
class LabCache {
    private static final Logger logger = LoggerFactory.getLogger(LabCache.class);
    private final LabTestDefinitionRepository labTestDefinitionRepo;
    private final LabBatteryElementRepository labBatteryElementRepo;


    /**
     * @param labTestDefinitionRepo repository for LabTestDefinition
     * @param labBatteryElementRepo repository for LabBatterElement
     */
    LabCache(LabTestDefinitionRepository labTestDefinitionRepo, LabBatteryElementRepository labBatteryElementRepo) {
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labBatteryElementRepo = labBatteryElementRepo;
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
        logger.trace("** Querying Lab test definition {} from labProvider {}", testLabCode, labProvider);
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

    /**
     * @param testDefinition test definition entity
     * @param battery        lab battery entity
     * @param validFrom      most recent change to results
     * @param storedFrom     time that star started processing the message
     */
    @Cacheable(value = "labBatteryElement", key = "{ #testDefinition.labTestDefinitionId , #battery.labBatteryId }")
    public void createLabBatteryElementIfNotExists(LabTestDefinition testDefinition, LabBattery battery, Instant validFrom, Instant storedFrom) {
        labBatteryElementRepo
                .findByLabBatteryIdAndLabTestDefinitionId(battery, testDefinition)
                .orElseGet(() -> {
                    LabBatteryElement batteryElement = new LabBatteryElement(testDefinition, battery, validFrom, storedFrom);
                    logger.trace("Creating new Lab Test Battery Element {}", batteryElement);
                    return labBatteryElementRepo.save(batteryElement);
                });
    }
}
