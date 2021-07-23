package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageCancelledException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleAudit;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;
import java.util.Optional;

/**
 * Controller for lab tables that aren't dependent on results.
 * @author Stef Piatek
 */
@Component
public class LabOrderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabBatteryRepository labBatteryRepo;
    private final LabSampleRepository labSampleRepo;
    private final LabSampleAuditRepository labSampleAuditRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabResultRepository labResultRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;
    private final QuestionController questionController;


    public LabOrderController(
            LabBatteryRepository labBatteryRepo, LabSampleRepository labSampleRepo,
            LabSampleAuditRepository labSampleAuditRepo, LabOrderRepository labOrderRepo, LabResultRepository labResultRepo,
            LabOrderAuditRepository labOrderAuditRepo, QuestionController questionController) {
        this.labBatteryRepo = labBatteryRepo;
        this.labSampleRepo = labSampleRepo;
        this.labSampleAuditRepo = labSampleAuditRepo;
        this.labOrderRepo = labOrderRepo;
        this.labResultRepo = labResultRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.questionController = questionController;
        createCoPathBattery();
    }

    private void createCoPathBattery() {
        String coPathName = OrderCodingSystem.CO_PATH.name();
        Instant now = Instant.now();
        LabBattery coPathBattery = getOrCreateLabBattery(coPathName, coPathName, now, now);
        coPathBattery.setDescription(new StringBuilder(2)
                .append("CoPath does not use test batteries, all orders are filed under this battery code. ")
                .append("The lab test definition lab department can be used to distinguish types of requested tests").toString());
        labBatteryRepo.save(coPathBattery);
    }


    /**
     * @param batteryCode  battery code
     * @param codingSystem coding system that battery is defined by
     * @param validFrom    most recent change to results
     * @param storedFrom   time that star started processing the message
     * @return lab battery
     */
    @Transactional
    @Cacheable(value = "labBattery", key = "{ #batteryCode, #codingSystem }")
    public LabBattery getOrCreateLabBattery(String batteryCode, String codingSystem, Instant validFrom, Instant storedFrom) {
        return labBatteryRepo.findByBatteryCodeAndLabProvider(batteryCode, codingSystem)
                .orElseGet(() -> {
                    LabBattery labBattery = new LabBattery(batteryCode, codingSystem, validFrom, storedFrom);
                    logger.trace("Creating new lab battery {}", labBattery);
                    return labBatteryRepo.save(labBattery);
                });
    }

    /**
     * Process lab number and lab labSample information, (including questions) returning the lab number.
     * @param mrn        MRN entity
     * @param visit      hospital visit entity
     * @param battery    Lab battery entity
     * @param msg        lab order msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return lab number entity
     * @throws MessageCancelledException Lab Order was previously cancelled
     */
    @Transactional
    public LabOrder processSampleAndOrderInformation(
            Mrn mrn, HospitalVisit visit, LabBattery battery, LabOrderMsg msg, Instant validFrom, Instant storedFrom
    ) throws MessageCancelledException {
        LabSample labSample = updateOrCreateSample(mrn, msg, validFrom, storedFrom);
        questionController.processQuestions(msg.getQuestions(), labSample.getLabSampleId(), validFrom, storedFrom);
        return updateOrCreateLabOrder(visit, battery, labSample, msg, validFrom, storedFrom);
    }

    /**
     * @param mrn        MRN entity
     * @param battery    Lab battery entity
     * @param visit      hospital visit entity
     * @param msg        lab order msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @throws IncompatibleDatabaseStateException if message has already been deleted
     */
    @Transactional
    public void processLabSampleAndDeleteLabOrder(
            Mrn mrn, LabBattery battery, HospitalVisit visit, LabOrderMsg msg, Instant validFrom, Instant storedFrom
    ) throws IncompatibleDatabaseStateException {
        LabSample labSample = updateOrCreateSample(mrn, msg, validFrom, storedFrom);
        deleteLabOrderIfExistsAndNewer(visit, battery, labSample, msg, validFrom, storedFrom);
    }

    /**
     * Create new LabSample or update existing one.
     * @param mrnId      MRN entity
     * @param msg        Msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return LabSample entity
     */
    private LabSample updateOrCreateSample(
            Mrn mrnId, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabSample, LabSampleAudit> state = labSampleRepo
                .findByMrnIdAndExternalLabNumber(mrnId, msg.getLabSpecimenNumber())
                .map(col -> new RowState<>(col, validFrom, storedFrom, false))
                .orElseGet(() -> createLabSample(mrnId, msg.getLabSpecimenNumber(), validFrom, storedFrom));

        LabSample labSample = state.getEntity();
        state.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getSpecimenType(), labSample.getSpecimenType(), labSample::setSpecimenType, validFrom, labSample.getValidFrom());
        state.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getSampleSite(), labSample.getSampleSite(), labSample::setSampleSite, validFrom, labSample.getValidFrom());
        state.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getSampleReceivedTime(), labSample.getReceiptAtLab(), labSample::setReceiptAtLab, validFrom, labSample.getValidFrom());
        // Allow for change of sample labSample time, but don't expect this to happen
        if (state.isEntityCreated() || validFrom.isAfter(labSample.getValidFrom())) {
            if (collectionTimeExistsAndWillChange(msg, labSample)) {
                logger.warn("Not expecting Sample Collection time to change");
            }
            state.assignIfDifferent(msg.getCollectionDateTime(), labSample.getSampleCollectionTime(), labSample::setSampleCollectionTime);
            state.assignInterchangeValue(msg.getCollectionMethod(), labSample.getCollectionMethod(), labSample::setCollectionMethod);
        }

        state.saveEntityOrAuditLogIfRequired(labSampleRepo, labSampleAuditRepo);
        return labSample;
    }

    private boolean collectionTimeExistsAndWillChange(LabOrderMsg msg, LabSample labSample) {
        return labSample.getSampleCollectionTime() != null && !labSample.getSampleCollectionTime().equals(msg.getCollectionDateTime());
    }

    private RowState<LabSample, LabSampleAudit> createLabSample(Mrn mrn, String externalLabNumber, Instant validFrom, Instant storedFrom) {
        LabSample labSample = new LabSample(mrn, externalLabNumber, validFrom, storedFrom);
        return new RowState<>(labSample, validFrom, storedFrom, true);
    }

    /**
     * Create new lab order or update existing one.
     * As temporal data can come from different sources, update each one if it is currently null, or the message is newer and has a different value.
     * @param visit      Optional Hospital Visit Entity
     * @param battery    Lab Battery
     * @param labSample  Lab Sample entity
     * @param msg        Msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return Lab Order
     * @throws MessageCancelledException If message has previously been cancelled
     */
    private LabOrder updateOrCreateLabOrder(
            @Nullable HospitalVisit visit, LabBattery battery, LabSample labSample, LabOrderMsg msg, Instant validFrom, Instant storedFrom)
            throws MessageCancelledException {
        RowState<LabOrder, LabOrderAudit> orderState = labOrderRepo
                .findByLabBatteryIdAndLabSampleId(battery, labSample)
                .map(order -> new RowState<>(order, validFrom, storedFrom, false))
                .orElseGet(() -> createLabOrder(battery, labSample, validFrom, storedFrom));

        if (orderState.isEntityCreated()
                && labOrderAuditRepo.previouslyDeleted(
                battery.getLabBatteryId(), labSample.getLabSampleId(), msg.getOrderDateTime(), msg.getEpicCareOrderNumber())) {
            throw new MessageCancelledException("Message previously cancelled");
        }

        updateLabOrder(visit, msg, validFrom, orderState);
        orderState.saveEntityOrAuditLogIfRequired(labOrderRepo, labOrderAuditRepo);
        return orderState.getEntity();
    }

    private RowState<LabOrder, LabOrderAudit> createLabOrder(LabBattery battery, LabSample labSample, Instant validFrom, Instant storedFrom) {
        LabOrder order = new LabOrder(battery, labSample);
        order.setValidFrom(validFrom);
        order.setStoredFrom(storedFrom);
        return new RowState<>(order, validFrom, storedFrom, true);
    }

    private void updateLabOrder(HospitalVisit visit, LabOrderMsg msg, Instant validFrom, RowState<LabOrder, LabOrderAudit> orderState) {
        LabOrder order = orderState.getEntity();

        // Values that should always update if they're null
        if (order.getHospitalVisitId() == null) {
            orderState.assignIfDifferent(visit, null, order::setHospitalVisitId);
        }


        orderState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getOrderDateTime(), order.getOrderDatetime(), order::setOrderDatetime, validFrom, order.getValidFrom());
        orderState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getRequestedDateTime(), order.getRequestDatetime(), order::setRequestDatetime, validFrom, order.getValidFrom());

        // only update if newer
        if (orderState.isEntityCreated() || validFrom.isAfter(order.getValidFrom())) {
            orderState.assignInterchangeValue(msg.getClinicalInformation(), order.getClinicalInformation(), order::setClinicalInformation);
            orderState.assignIfDifferent(msg.getSourceSystem(), order.getSourceSystem(), order::setSourceSystem);
            if (epicNumberIsSaveAndDifferent(msg, order)) {
                logger.warn("Epic lab order number has changed from {} to {}", order.getInternalLabNumber(), msg.getEpicCareOrderNumber().get());
            }
            orderState.assignInterchangeValue(msg.getEpicCareOrderNumber(), order.getInternalLabNumber(), order::setInternalLabNumber);
        }
    }

    private boolean epicNumberIsSaveAndDifferent(LabOrderMsg msg, LabOrder order) {
        return msg.getEpicCareOrderNumber().isSave()
                && order.getInternalLabNumber() != null
                && !msg.getEpicCareOrderNumber().get().equals(order.getInternalLabNumber());
    }

    /**
     * @param visit      visit entity
     * @param battery    lab battery entity
     * @param labSample  lab sample entity
     * @param msg        order message
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @throws IncompatibleDatabaseStateException if order already has results and a delete is attempted
     */
    private void deleteLabOrderIfExistsAndNewer(
            HospitalVisit visit, LabBattery battery, LabSample labSample, LabOrderMsg msg, Instant validFrom, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        Optional<LabOrder> possibleLabOrder = labOrderRepo.findByLabBatteryIdAndLabSampleIdAndValidFromBefore(battery, labSample, validFrom);
        LabOrder labOrder;
        if (possibleLabOrder.isPresent()) {
            labOrder = possibleLabOrder.get();
            if (labResultRepo.existsByLabOrderId(labOrder)) {
                throw new IncompatibleDatabaseStateException("Delete message for order that already has results, not deleting");
            }
        } else {
            // build a lab order to create an audit row without saving the original lab order
            RowState<LabOrder, LabOrderAudit> orderState = createLabOrder(battery, labSample, validFrom, storedFrom);
            updateLabOrder(visit, msg, validFrom, orderState);
            labOrder = orderState.getEntity();
        }
        LabOrderAudit orderAudit = labOrder.createAuditEntity(validFrom, storedFrom);
        labOrderAuditRepo.save(orderAudit);
        logger.debug("Deleting LabOrder {}", labOrder);
        labOrderRepo.delete(labOrder);
    }
}
