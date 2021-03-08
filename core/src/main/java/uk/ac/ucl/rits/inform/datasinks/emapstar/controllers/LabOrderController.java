package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleAudit;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;
import java.util.function.Consumer;

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
    private final LabOrderAuditRepository labOrderAuditRepo;


    public LabOrderController(
            LabBatteryRepository labBatteryRepo, LabSampleRepository labSampleRepo,
            LabSampleAuditRepository labSampleAuditRepo, LabOrderRepository labOrderRepo, LabOrderAuditRepository labOrderAuditRepo
    ) {
        this.labBatteryRepo = labBatteryRepo;
        this.labSampleRepo = labSampleRepo;
        this.labSampleAuditRepo = labSampleAuditRepo;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
    }


    /**
     * @param batteryCode  battery code
     * @param codingSystem coding system that battery is defined by
     * @param validFrom    most recent change to results
     * @param storedFrom   time that star started processing the message
     * @return lab battery
     */
    @Transactional
    public LabBattery getOrCreateLabBattery(String batteryCode, String codingSystem, Instant validFrom, Instant storedFrom) {
        return labBatteryRepo.findByBatteryCodeAndLabProvider(batteryCode, codingSystem)
                .orElseGet(() -> {
                    LabBattery labBattery = new LabBattery(batteryCode, codingSystem, validFrom, storedFrom);
                    logger.trace("Creating new lab battery {}", labBattery);
                    return labBatteryRepo.save(labBattery);
                });
    }

    /**
     * Process lab number and lab labSample information, returning the lab number.
     * @param mrn        MRN entity
     * @param visit      hospital visit entity
     * @param battery    Lab battery entity
     * @param msg        lab order msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return lab number entity
     * @throws IncompatibleDatabaseStateException if specimen type or sample type doesn't match the database
     */
    @Transactional
    public LabOrder processLabSampleAndLabOrder(
            Mrn mrn, HospitalVisit visit, LabBattery battery, LabOrderMsg msg, Instant validFrom, Instant storedFrom
    ) throws IncompatibleDatabaseStateException {
        LabSample labSample = updateOrCreateSample(mrn, msg, validFrom, storedFrom);
        return updateOrCreateLabOrder(visit, battery, labSample, msg, validFrom, storedFrom);
    }

    @Transactional
    public void processLabSampleAndDeleteLabOrder(
            Mrn mrn, LabBattery battery, LabOrderMsg msg, Instant validFrom, Instant storedFrom
    ) throws IncompatibleDatabaseStateException {
        LabSample labSample = updateOrCreateSample(mrn, msg, validFrom, storedFrom);
        deleteLabOrderIfExistsAndNewer(battery, labSample, validFrom, storedFrom);
    }

    /**
     * Create new LabSample or update existing one.
     * @param mrnId      MRN entity
     * @param msg        Msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return LabSample entity
     * @throws IncompatibleDatabaseStateException if specimen type or sample site changes
     */
    private LabSample updateOrCreateSample(
            Mrn mrnId, LabOrderMsg msg, Instant validFrom, Instant storedFrom) throws IncompatibleDatabaseStateException {
        RowState<LabSample, LabSampleAudit> state = labSampleRepo
                .findByMrnIdAndExternalLabNumber(mrnId, msg.getLabSpecimenNumber())
                .map(col -> new RowState<>(col, validFrom, storedFrom, false))
                .orElseGet(() -> createLabSample(mrnId, msg.getLabSpecimenNumber(), validFrom, storedFrom));

        LabSample labSample = state.getEntity();

        assignIfCurrentlyNullOrNewerAndDifferent(
                state, msg.getSampleReceivedTime(), labSample.getReceiptAtLab(), labSample::setReceiptAtLab, validFrom, labSample.getValidFrom());
        assignIfCurrentlyNullOrThrowIfDifferent(state, msg.getSpecimenType(), labSample.getSpecimenType(), labSample::setSpecimenType);
        assignIfCurrentlyNullOrThrowIfDifferent(state, msg.getSampleSite(), labSample.getSampleSite(), labSample::setSampleSite);
        // Allow for change of sample labSample time, but don't expect this to happen
        if (state.isEntityCreated() || validFrom.isAfter(labSample.getValidFrom())) {
            if (collectionTimeExistsAndWillChange(msg, labSample)) {
                logger.warn("Not expecting Sample Collection time to change");
            }
            state.assignIfDifferent(msg.getCollectionDateTime(), labSample.getSampleCollectionTime(), labSample::setSampleCollectionTime);
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

    private void assignIfCurrentlyNullOrThrowIfDifferent(
            RowState<?, ?> state, InterchangeValue<String> msgValue, String currentValue, Consumer<String> setter
    ) throws IncompatibleDatabaseStateException {
        if (currentValue == null) {
            state.assignInterchangeValue(msgValue, currentValue, setter);
        } else if (msgValue.isSave() && !msgValue.get().equals(currentValue)) {
            throw new IncompatibleDatabaseStateException(String.format("Current value %s different from message %s", currentValue, msgValue.get()));
        }
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
     * @throws IncompatibleDatabaseStateException If the message epic order number is known and is different from the database's internal lab number.
     *                                            Could happen if message are received out of order
     *                                            (a battery is ordered for a sample, cancelled and then the same battery is ordered again;
     *                                            we receive order 1, order 2 before the cancel)
     */
    private LabOrder updateOrCreateLabOrder(
            @Nullable HospitalVisit visit, LabBattery battery, LabSample labSample, LabOrderMsg msg, Instant validFrom, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        RowState<LabOrder, LabOrderAudit> orderState = labOrderRepo
                .findByLabBatteryIdAndLabSampleId(battery, labSample)
                .map(order -> new RowState<>(order, validFrom, storedFrom, false))
                .orElseGet(() -> createLabOrder(battery, labSample, validFrom, storedFrom));

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

    private void updateLabOrder(HospitalVisit visit, LabOrderMsg msg, Instant validFrom, RowState<LabOrder, LabOrderAudit> orderState)
            throws IncompatibleDatabaseStateException {
        LabOrder order = orderState.getEntity();

        if (order.getHospitalVisitId() == null) {
            orderState.assignIfDifferent(visit, null, order::setHospitalVisitId);
        }

        // Values that should always update if they're null
        assignIfCurrentlyNullOrNewerAndDifferent(
                orderState, msg.getOrderDateTime(), order.getOrderDatetime(), order::setOrderDatetime, validFrom, order.getValidFrom());
        assignIfCurrentlyNullOrNewerAndDifferent(
                orderState, msg.getRequestedDateTime(), order.getRequestDatetime(), order::setRequestDatetime, validFrom, order.getValidFrom());
        assignIfCurrentlyNullOrThrowIfDifferent(orderState, msg.getEpicCareOrderNumber(), order.getInternalLabNumber(), order::setInternalLabNumber);

        // only update if newer
        if (orderState.isEntityCreated() || validFrom.isAfter(order.getValidFrom())) {
            orderState.assignInterchangeValue(msg.getClinicalInformation(), order.getClinicalInformation(), order::setClinicalInformation);
            orderState.assignIfDifferent(msg.getSourceSystem(), order.getSourceSystem(), order::setSourceSystem);
        }
    }

    private void assignIfCurrentlyNullOrNewerAndDifferent(
            RowState<?, ?> state, InterchangeValue<Instant> msgValue, Instant currentValue, Consumer<Instant> setter,
            Instant messageValidFrom, Instant entityValidFrom
    ) {
        if (currentValue == null || messageValidFrom.isAfter(entityValidFrom)) {
            state.assignInterchangeValue(msgValue, currentValue, setter);
        }
    }

    private void deleteLabOrderIfExists(LabBattery battery, LabSample labSample, Instant validFrom, Instant storedFrom) {
        labOrderRepo.findByLabBatteryIdAndLabSampleId(battery, labSample)
                .ifPresent(order -> {
                    LabOrderAudit orderAudit = order.createAuditEntity(validFrom, storedFrom);
                    labOrderAuditRepo.save(orderAudit);
                    logger.debug("Deleting labOrder {}", order);
                    labOrderRepo.delete(order);
                });
    }
}
