package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageCancelledException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinitionAudit;
import uk.ac.ucl.rits.inform.interchange.lab.LabMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Main class that interacts with labs tables, either directly or through sub controllers.
 * @author Stef Piatek
 */
@Component
public class LabController {

    @Resource
    private LabCache cache;

    private static final Logger logger = LoggerFactory.getLogger(LabController.class);

    private final LabOrderController labOrderController;
    private final LabResultController labResultController;


    /**
     * @param labOrderController  controller for LabOrder tables
     * @param labResultController controller for LabResult tables
     */
    public LabController(LabOrderController labOrderController, LabResultController labResultController) {
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

            LabTestDefinition testDefinition = updateOrCreateTestDefinitionWithLabDepartment(
                    msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode(), validFrom, storedFrom);
            cache.createLabBatteryElementIfNotExists(testDefinition, battery, storedFrom, validFrom);
            labResultController.processResult(testDefinition, labOrder, result, validFrom, storedFrom);
        }
    }


    /**
     * Lab results contain the specific reference to lab department so add this in if we don't already have it.
     * @param codingSystem  coding system, or interface that is sending the lab result
     * @param labDepartment department issuing the result
     * @param testCode      code for the test
     * @param validFrom     time in the hospital that the message was generated
     * @param storedFrom    time that star started processing the message
     * @return lab test definition
     */
    private LabTestDefinition updateOrCreateTestDefinitionWithLabDepartment(
            String codingSystem, String labDepartment, String testCode, Instant validFrom, Instant storedFrom) {
        RowState<LabTestDefinition, LabTestDefinitionAudit> definitionState = getOrCreateLabTestDefinition(
                codingSystem, testCode, validFrom, storedFrom
        );
        LabTestDefinition testDefinition = definitionState.getEntity();

        if (definitionState.isEntityCreated() || !Objects.equals(testDefinition.getLabDepartment(), labDepartment)) {
            definitionState.assignIfDifferent(labDepartment, testDefinition.getLabDepartment(), testDefinition::setLabDepartment);
            cache.updateLabTestDefinitionCache(definitionState);
        }

        return testDefinition;
    }

    private RowState<LabTestDefinition, LabTestDefinitionAudit> getOrCreateLabTestDefinition(
            String labProvider, String testLabCode, Instant validFrom, Instant storedFrom) {
        try {
            LabTestDefinition testDefinition = cache.findExistingLabTestDefinition(labProvider, testLabCode);
            return new RowState<>(testDefinition, validFrom, storedFrom, false);
        } catch (NoSuchElementException e) {
            // doesn't exist in database or cache, so create a new one
            // shouldn't cache the row state, just the entity so having to do this using exception handling
            LabTestDefinition testDefinition = new LabTestDefinition(labProvider, testLabCode);
            logger.trace("Creating new Lab Test Definition {}", testDefinition);
            return new RowState<>(testDefinition, validFrom, storedFrom, true);
        }
    }

    /**
     * Flesh out lab battery or lab test metadata, but not battery element.
     * <p>
     * We don't get information about *which* tests are in which batteries from Clarity.
     * We can only infer this from the test results we get.
     * So Lab Metadata is going to be about fleshing out the battery and test data we do have,
     * @param labMetadataMsg lab metadata message
     * @param storedFrom     stored from timestamp
     * @throws RequiredDataMissingException if message type is not recognised
     */
    public void processLabMetadata(LabMetadataMsg labMetadataMsg, Instant storedFrom) throws RequiredDataMissingException {
        switch (labMetadataMsg.getLabsMetadataType()) {
            case LABS_METADATA_BATTERY:
                // The code AND the coding system are used to lookup here
                updateOrCreateLabBatteryMetadata(labMetadataMsg, storedFrom);
                break;
            case LABS_METADATA_TEST:
                updateOrCreateTestDefinitionWithName(labMetadataMsg, labMetadataMsg.getValidFrom(), storedFrom);
                break;
            default:
                throw new RequiredDataMissingException(
                        String.format("Unrecognised lab metadata message type: %s", labMetadataMsg.getLabsMetadataType()));
        }
    }

    private void updateOrCreateLabBatteryMetadata(LabMetadataMsg labMetadataMsg, Instant storedFrom) {
        Instant validFrom = labMetadataMsg.getValidFrom();
        RowState<LabBattery, LabBatteryAudit> labBatteryRowState;
        LabBattery battery;
        try {
            battery = labOrderController.findLabBatteryOrThrow(labMetadataMsg.getShortCode(), labMetadataMsg.getCodingSystem().toString());
            labBatteryRowState = new RowState<>(battery, validFrom, storedFrom, false);
        } catch (NoSuchElementException e) {
            battery = new LabBattery(labMetadataMsg.getShortCode(), labMetadataMsg.getCodingSystem().toString(), validFrom, storedFrom);
            labBatteryRowState = new RowState<>(battery, validFrom, storedFrom, true);
        }

        if (labBatteryRowState.isEntityCreated() || battery.getBatteryName() == null || battery.getValidFrom().isBefore(validFrom)) {
            labBatteryRowState.assignIfDifferent(labMetadataMsg.getName(), battery.getBatteryName(), battery::setBatteryName);
            cache.saveEntityAndUpdateCache(labBatteryRowState);
        }
    }

    /**
     * Lab results metadata have a test name that doesn't exist in the hl7 feed.
     * @param msg        Lab metadata message that should update the current data
     * @param validFrom  time in the hospital that the message was generated
     * @param storedFrom time that star started processing the message
     */
    private void updateOrCreateTestDefinitionWithName(LabMetadataMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabTestDefinition, LabTestDefinitionAudit> definitionState = getOrCreateLabTestDefinition(
                msg.getCodingSystem().toString(), msg.getShortCode(), validFrom, storedFrom
        );
        LabTestDefinition testDefinition = definitionState.getEntity();

        if (definitionState.isEntityCreated() || testDefinition.getName() == null || testDefinition.getValidFrom().isBefore(validFrom)) {
            definitionState.assignIfDifferent(msg.getName(), testDefinition.getName(), testDefinition::setName);
            cache.updateLabTestDefinitionCache(definitionState);
        }
    }

    /**
     * Deletes lab orders that are older than the current message, along with tables which require orders.
     * @param visit             Hospital Visit Entity
     * @param invalidationTime  Lab Battery
     * @param deletionTime      Lab Sample entity
     */
    public void deleteLabOrdersForVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        List<LabOrder> labOrders = labOrderController.getLabOrdersForVisit(visit);
        for (var lo : labOrders) {
            labResultController.deleteLabResultsForLabOrder(lo, invalidationTime, deletionTime);
            labOrderController.deleteLabOrder(lo, invalidationTime, deletionTime);
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
    private final LabTestDefinitionAuditRepository labTestDefinitionAuditRepo;
    private final LabBatteryElementRepository labBatteryElementRepo;
    private final LabBatteryRepository labBatteryRepository;
    private final LabBatteryAuditRepository labBatteryAuditRepository;


    /**
     * @param labTestDefinitionRepo      repository for LabTestDefinition
     * @param labTestDefinitionAuditRepo to audit changes to lab test definitions
     * @param labBatteryElementRepo      repository for LabBatteryElement
     * @param labBatteryRepository       repository for LabBattery
     * @param labBatteryAuditRepository  repository for LabBatteryAudit
     */
    LabCache(LabTestDefinitionRepository labTestDefinitionRepo,
             LabTestDefinitionAuditRepository labTestDefinitionAuditRepo,
             LabBatteryElementRepository labBatteryElementRepo,
             LabBatteryRepository labBatteryRepository,
             LabBatteryAuditRepository labBatteryAuditRepository) {
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labTestDefinitionAuditRepo = labTestDefinitionAuditRepo;
        this.labBatteryElementRepo = labBatteryElementRepo;
        this.labBatteryRepository = labBatteryRepository;
        this.labBatteryAuditRepository = labBatteryAuditRepository;
    }


    /**
     * @param testLabCode local result item code
     * @param labProvider battery coding system
     * @return LabTestDefinition entity
     * @throws NoSuchElementException if the test definition doesn't exist
     */
    @Cacheable(value = "labTestDefinition", key = "{ #labProvider , #testLabCode }")
    public LabTestDefinition findExistingLabTestDefinition(String labProvider, String testLabCode) {
        logger.trace("** Querying Lab test definition '{}' from labProvider '{}'", testLabCode, labProvider);
        return labTestDefinitionRepo.findByLabProviderAndTestLabCode(labProvider, testLabCode).orElseThrow();
    }

    /**
     * Save entity if require and update cache with the LabTestDefinition entity.
     * @param definitionState to save and then cache
     * @return testDefinition for the cache.
     */
    @CachePut(value = "labTestDefinition", key = "{ #definitionState.entity.labProvider , #definitionState.entity.testLabCode }")
    public LabTestDefinition updateLabTestDefinitionCache(RowState<LabTestDefinition, LabTestDefinitionAudit> definitionState) {
        LabTestDefinition testDefinition = definitionState.getEntity();
        logger.trace("** Overwriting cache value for Lab test definition '{}' from labProvider '{}'",
                testDefinition.getTestLabCode(), testDefinition.getLabProvider());
        definitionState.saveEntityOrAuditLogIfRequired(labTestDefinitionRepo, labTestDefinitionAuditRepo);
        return testDefinition;
    }

    /**
     * @param testDefinition test definition entity
     * @param battery        lab battery entity
     * @param validFrom      most recent change to results
     * @param storedFrom     time that star started processing the message
     * @return Lab Battery element for cache
     */
    @Cacheable(value = "labBatteryElement", key = "{ #testDefinition.labTestDefinitionId , #battery.labBatteryId }")
    public LabBatteryElement createLabBatteryElementIfNotExists(
            LabTestDefinition testDefinition, LabBattery battery, Instant validFrom, Instant storedFrom) {
        return labBatteryElementRepo
                .findByLabBatteryIdAndLabTestDefinitionId(battery, testDefinition)
                .orElseGet(() -> {
                    LabBatteryElement batteryElement = new LabBatteryElement(testDefinition, battery, validFrom, storedFrom);
                    logger.trace("Creating new Lab Test Battery Element {}", batteryElement);
                    return labBatteryElementRepo.save(batteryElement);
                });
    }

    /**
     * Save and overwrite cache for LabBattery.
     * @param definitionState to update and then cache the LabBattery
     * @return LabBatter for the cache
     */
    @CachePut(value = "labBattery",
            key = "{ #definitionState.getEntity().getBatteryCode(), #definitionState.getEntity().getLabProvider() }")
    public LabBattery saveEntityAndUpdateCache(RowState<LabBattery, LabBatteryAudit> definitionState) {
        logger.trace("** Overwriting cache value for battery '{}' for lab provider '{}'",
                definitionState.getEntity().getBatteryCode(), definitionState.getEntity().getLabProvider());
        definitionState.saveEntityOrAuditLogIfRequired(labBatteryRepository, labBatteryAuditRepository);
        return definitionState.getEntity();
    }
}
