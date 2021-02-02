package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabCollectionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabCollectionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollection;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollectionAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
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
    private final LabCollectionRepository labCollectionRepo;
    private final LabCollectionAuditRepository labCollectionAuditRepository;
    private final LabResultController labResultController;


    public LabController(
            LabNumberRepository labNumberRepo, LabCollectionRepository labCollectionRepo,
            LabCollectionAuditRepository labCollectionAuditRepository, LabResultController labResultController) {
        this.labNumberRepo = labNumberRepo;
        this.labCollectionRepo = labCollectionRepo;
        this.labCollectionAuditRepository = labCollectionAuditRepository;
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

        LabNumber labNumber = getOrCreateLabNumber(mrn, visit, msg, storedFrom);
        Instant validFrom = msg.getStatusChangeTime();
        RowState<LabCollection, LabCollectionAudit> collectionState = updateOrCreateLabCollection(labNumber, msg, validFrom, storedFrom);
        for (LabResultMsg result : msg.getLabResultMsgs()) {
            labResultController.processResult(msg, labNumber, result, validFrom, storedFrom);
        }
    }

    /**
     * @param mrn        MRN
     * @param visit      hospital visit, can be null
     * @param msg        order message
     * @param storedFrom time that star started processing the message
     * @return lab number
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
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

        return labNumber;
    }

    /**
     * Create new LabCollection or update existing one.
     * @param labNumber  LabNumber
     * @param msg        Msg
     * @param validFrom  most recent change to results
     * @param storedFrom time that star encountered the message
     * @return Lab Collection wrapped in row state
     */
    private RowState<LabCollection, LabCollectionAudit> updateOrCreateLabCollection(
            LabNumber labNumber, LabOrderMsg msg, Instant validFrom, Instant storedFrom) {
        RowState<LabCollection, LabCollectionAudit> state = labCollectionRepo
                .findByLabNumberIdAndSampleType(labNumber, msg.getSpecimenType())
                .map(col -> new RowState<>(col, validFrom, storedFrom, false))
                .orElseGet(() -> createLabCollection(labNumber, msg.getSpecimenType(), validFrom, storedFrom));

        LabCollection collection = state.getEntity();
        // If no sample received time or more recent message, try to update it
        if (collection.getSampleReceiptTime() == null || collection.getValidFrom().isBefore(validFrom)) {
            state.assignInterchangeValue(msg.getSampleReceivedTime(), collection.getSampleReceiptTime(), collection::setSampleReceiptTime);
        }
        // Allow for change of sample collection time, but don't expect this to happen
        if (state.isEntityCreated() || collection.getValidFrom().isBefore(validFrom)) {
            state.assignIfDifferent(msg.getCollectionDateTime(), collection.getSampleCollectionTime(), collection::setSampleCollectionTime);
            if (!state.isEntityCreated()) {
                logger.warn("Not expecting Sample collection time to change");
            }
        }

        state.saveEntityOrAuditLogIfRequired(labCollectionRepo, labCollectionAuditRepository);
        return state;
    }

    private RowState<LabCollection, LabCollectionAudit> createLabCollection(
            LabNumber labNumber, String specimenType, Instant validFrom, Instant storedFrom) {
        LabCollection collection = new LabCollection(labNumber, specimenType);
        collection.setValidFrom(validFrom);
        collection.setStoredFrom(storedFrom);
        return new RowState<>(collection, validFrom, storedFrom, true);
    }
}
