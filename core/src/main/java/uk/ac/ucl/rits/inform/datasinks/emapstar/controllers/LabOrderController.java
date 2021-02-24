package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabCollectionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabCollectionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollection;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollectionAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
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

    private final LabNumberRepository labNumberRepo;
    private final LabBatteryRepository labBatteryRepository;
    private final LabCollectionRepository labCollectionRepo;
    private final LabCollectionAuditRepository labCollectionAuditRepository;
    private final LabOrderRepository labOrderRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;


    public LabOrderController(
            LabNumberRepository labNumberRepo, LabBatteryRepository labBatteryRepository, LabCollectionRepository labCollectionRepo,
            LabCollectionAuditRepository labCollectionAuditRepository, LabOrderRepository labOrderRepo, LabOrderAuditRepository labOrderAuditRepo
    ) {
        this.labNumberRepo = labNumberRepo;
        this.labBatteryRepository = labBatteryRepository;
        this.labCollectionRepo = labCollectionRepo;
        this.labCollectionAuditRepository = labCollectionAuditRepository;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
    }


    /**
     * @param msg        Lab order msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star started processing the message
     * @return lab battery
     */
    @Transactional
    public LabBattery getOrCreateLabBattery(LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        return labBatteryRepository.findByBatteryCodeAndLabProvider(msg.getTestBatteryLocalCode(), msg.getTestBatteryCodingSystem())
                .orElseGet(() -> {
                    LabBattery labBattery = new LabBattery(msg.getTestBatteryLocalCode(), msg.getTestBatteryCodingSystem(), validFrom, storedFrom);
                    logger.trace("Creating new lab battery {}", labBattery);
                    return labBatteryRepository.save(labBattery);
                });
    }

    /**
     * Process lab number and lab collection information, returning the lab number.
     * @param mrn        MRN entity
     * @param visit      hospital visit entity
     * @param battery    Lab battery entity
     * @param msg        lab order msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return lab number entity
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
     */
    @Transactional
    public LabNumber processLabNumberLabCollectionAndLabOrder(Mrn mrn, HospitalVisit visit, LabBattery battery, LabOrderMsg msg, Instant validFrom, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        LabNumber labNumber = getOrCreateLabNumber(mrn, visit, msg, storedFrom);
        updateOrCreateLabCollection(labNumber, msg, validFrom, storedFrom);
        updateOrCreateLabOrder(battery, labNumber, msg, validFrom, storedFrom);
        return labNumber;
    }


    /**
     * @param mrn        MRN
     * @param visit      hospital visit, can be null
     * @param msg        order message
     * @param storedFrom time that star started processing the message
     * @return lab number
     * @throws IncompatibleDatabaseStateException if source system doesn't match the database
     */
    private LabNumber getOrCreateLabNumber(
            Mrn mrn, @Nullable HospitalVisit visit, LabOrderMsg msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        LabNumber labNumber = labNumberRepo
                .findByMrnIdAndHospitalVisitIdAndInternalLabNumberAndExternalLabNumber(
                        mrn, visit, msg.getLabSpecimenNumber(), msg.getEpicCareOrderNumber())
                .orElseGet(() -> {
                    LabNumber labNum = new LabNumber(
                            mrn, visit, msg.getLabSpecimenNumber(), msg.getEpicCareOrderNumber(),
                            msg.getSourceSystem(), storedFrom);
                    logger.trace("Creating new lab number {}", labNum);
                    return labNumberRepo.save(labNum);
                });
        // currently assuming that internal and external lab numbers are unique. If a source system has overlap, throw and error so
        // we know that we have to change our query to include source system (seems unlikely)
        if (!labNumber.getSourceSystem().equals(msg.getSourceSystem())) {
            throw new IncompatibleDatabaseStateException(
                    String.format("Source system in database for lab number %d is %s, message has value of %s",
                            labNumber.getLabNumberId(), labNumber.getSourceSystem(), msg.getSourceSystem()));
        }

        return labNumber;
    }

    /**
     * Create new LabCollection or update existing one.
     * @param labNumber  LabNumber
     * @param msg        Msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     */
    private void updateOrCreateLabCollection(
            LabNumber labNumber, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabCollection, LabCollectionAudit> state = labCollectionRepo
                .findByLabNumberIdAndSampleType(labNumber, msg.getSpecimenType())
                .map(col -> new RowState<>(col, validFrom, storedFrom, false))
                .orElseGet(() -> createLabCollection(labNumber, msg.getSpecimenType(), validFrom, storedFrom));

        LabCollection collection = state.getEntity();
        // If no sample received time or more recent message, try to update it
        if (collection.getReceiptAtLab() == null || collection.getValidFrom().isBefore(validFrom)) {
            state.assignInterchangeValue(msg.getSampleReceivedTime(), collection.getReceiptAtLab(), collection::setReceiptAtLab);
        }
        // Allow for change of sample collection time, but don't expect this to happen
        if (state.isEntityCreated() || collection.getValidFrom().isBefore(validFrom)) {
            state.assignIfDifferent(msg.getCollectionDateTime(), collection.getSampleCollectionTime(), collection::setSampleCollectionTime);
            if (!state.isEntityCreated()) {
                logger.warn("Not expecting Sample collection time to change");
            }
        }

        state.saveEntityOrAuditLogIfRequired(labCollectionRepo, labCollectionAuditRepository);
    }

    private RowState<LabCollection, LabCollectionAudit> createLabCollection(
            LabNumber labNumber, String specimenType, Instant validFrom, Instant storedFrom) {
        LabCollection collection = new LabCollection(labNumber, specimenType);
        collection.setValidFrom(validFrom);
        collection.setStoredFrom(storedFrom);
        return new RowState<>(collection, validFrom, storedFrom, true);
    }


    /**
     * Create new lab order or update existing one.
     * As temporal data can come from different sources, update each one if it is currently null, or the message is newer and has a different value.
     * @param battery    Lab Battery
     * @param labNumber  LabNumber
     * @param msg        Msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     */
    private void updateOrCreateLabOrder(
            LabBattery battery, LabNumber labNumber, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabOrder, LabOrderAudit> orderState = labOrderRepo
                .findByLabBatteryIdAndLabNumberId(battery, labNumber)
                .map(order -> new RowState<>(order, validFrom, storedFrom, false))
                .orElseGet(() -> createLabOrder(battery, labNumber, validFrom, storedFrom));

        updateLabOrder(orderState, msg, validFrom);
        orderState.saveEntityOrAuditLogIfRequired(labOrderRepo, labOrderAuditRepo);
    }

    private RowState<LabOrder, LabOrderAudit> createLabOrder(LabBattery battery, LabNumber labNumber, Instant validFrom, Instant storedFrom) {
        LabOrder order = new LabOrder(battery, labNumber);
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
