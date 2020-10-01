package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditHospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;

import java.time.Instant;

/**
 * Interactions with visits.
 */
@Component
public class VisitController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HospitalVisitRepository hospitalVisitRepo;
    private final AuditHospitalVisitRepository auditHospitalVisitRepo;

    public VisitController(HospitalVisitRepository hospitalVisitRepo, AuditHospitalVisitRepository auditHospitalVisitRepo) {
        this.hospitalVisitRepo = hospitalVisitRepo;
        this.auditHospitalVisitRepo = auditHospitalVisitRepo;
    }

    /**
     * Get or create hospital visit, should be used for non-ADT source.
     * Will create a minimum hospital visit and save it it can't match one by the encounter string.
     * @param encounter       encounter number
     * @param mrn             Mrn
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return Hospital visit from database or minimal hospital visit
     * @throws MessageIgnoredException if no encounter
     */
    public HospitalVisit getOrCreateMinimalHospitalVisit(final String encounter, final Mrn mrn, final String sourceSystem,
                                                         final Instant messageDateTime, final Instant storedFrom) throws MessageIgnoredException {
        RowState<HospitalVisit> visit = getOrCreateHospitalVisit(encounter, mrn, sourceSystem, messageDateTime, storedFrom);
        if (visit.isEntityCreated()) {
            logger.debug("Minimal encounter created. encounter: {}, mrn: {}", encounter, mrn);
            hospitalVisitRepo.save(visit.getEntity());
        }
        return visit.getEntity();
    }

    /**
     * Get or create minimal hospital visit, and update whether it was created.
     * @param encounter       encounter number
     * @param mrn             Mrn
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return existing visit or created minimal visit
     * @throws MessageIgnoredException if no encounter
     */
    private RowState<HospitalVisit> getOrCreateHospitalVisit(final String encounter, final Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                                             final Instant storedFrom) throws MessageIgnoredException {
        if (encounter == null || encounter.isEmpty()) {
            throw new MessageIgnoredException(String.format("No encounter for message. Mrn: %s, sourceSystem: %s, messageDateTime: %s",
                    mrn, sourceSystem, messageDateTime));
        }
        return hospitalVisitRepo.findByEncounter(encounter)
                .map(visit -> new RowState<HospitalVisit>(visit, messageDateTime, storedFrom, false))
                .orElseGet(() -> createHospitalVisit(encounter, mrn, sourceSystem, messageDateTime, storedFrom));
    }


    /**
     * Create minimal hospital visit.
     * @param encounter       encounter number
     * @param mrn             Mrn
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return new hospital visit
     */
    private RowState<HospitalVisit> createHospitalVisit(final String encounter, Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                                        final Instant storedFrom) {
        HospitalVisit visit = new HospitalVisit();
        visit.setMrnId(mrn);
        visit.setEncounter(encounter);
        visit.setSourceSystem(sourceSystem);
        visit.setStoredFrom(storedFrom);
        visit.setValidFrom(messageDateTime);
        return new RowState<>(visit, messageDateTime, storedFrom, true);
    }

    /**
     * Process information about hospital visits, saving any changes to the database.
     * @param msg             adt message
     * @param storedFrom      time that emap-core started processing the message.
     * @param messageDateTime date time of the message
     * @param mrn             mrn
     * @return hospital visit
     * @throws MessageIgnoredException
     */
    @Transactional
    public HospitalVisit updateOrCreateHospitalVisit(final AdtMessage msg, final Instant storedFrom, final Instant messageDateTime,
                                                     final Mrn mrn) throws MessageIgnoredException {
        if (msg.getVisitNumber() == null || msg.getVisitNumber().isEmpty()) {
            throw new MessageIgnoredException(String.format("ADT message doesn't have a visit number: %s", msg));
        }
        RowState<HospitalVisit> visitState = getOrCreateHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), msg.getRecordedDateTime(), storedFrom);
        final HospitalVisit originalVisit = visitState.getEntity().copy();
        if (messageShouldBeUpdated(messageDateTime, visitState)) {
            updateGenericData(msg, visitState);
            // process message based on the class type
            if (msg instanceof AdmitPatient) {
                AdmitPatient admit = (AdmitPatient) msg;
                addAdmissionInformation(admit, visitState);
            }
//            else if (msg instanceof RegisterPatient) {
//                RegisterPatient registerPatient = (RegisterPatient) msg;
//                addRegistrationInformation(registerPatient, storedFrom, visit);
//            }
//            // TODO: discharge
//            // TODO: cancel discharge
//            // TODO: cancel admit? to remove admission time or is this just location
//            // TODO: anything else that I'm missing
            manuallySaveVisitOrAuditIfRequired(visitState, originalVisit);
        }
        return visitState.getEntity();
    }

    /**
     * If message is newer than the database, newly created or if the database has data from untrusted source.
     * @param messageDateTime date time of the message
     * @param visitState      visit state
     * @return true if the message is newer or was created
     */
    private boolean messageShouldBeUpdated(final Instant messageDateTime, RowState<HospitalVisit> visitState) {
        return visitState.getEntity().getValidFrom().isBefore(messageDateTime) || visitState.isEntityCreated() || !visitState.getEntity().getSourceSystem().equals("EPIC");
    }

    /**
     * Update visit with generic ADT information.
     * @param msg        adt message
     * @param visitState hospital visit to update
     */
    public void updateGenericData(final AdtMessage msg, RowState<HospitalVisit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.assignHl7ValueIfDifferent(msg.getPatientClass(), visit.getPatientClass(), visit::setPatientClass);
        visitState.assignHl7ValueIfDifferent(msg.getModeOfArrival(), visit.getArrivalMethod(), visit::setArrivalMethod);
        visitState.assignIfDifferent(msg.getSourceSystem(), visit.getSourceSystem(), visit::setSourceSystem);
    }

    /**
     * Update visit with stored from and valid from, useful for any time the data has been updated.
     * @param msg        adt message
     * @param storedFrom stored from
     * @param visit      hospital visit to update
     */
    private void updateStoredFromAndValidFrom(final AdtMessage msg, final Instant storedFrom, HospitalVisit visit) {
        visit.setValidFrom(msg.getRecordedDateTime());
        visit.setStoredFrom(storedFrom);
    }

    /**
     * Add admission specific information.
     * @param msg        adt message
     * @param visitState
     */
    private void addAdmissionInformation(final AdmitPatient msg, RowState<HospitalVisit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.assignHl7ValueIfDifferent(msg.getAdmissionDateTime(), visit.getAdmissionTime(), visit::setAdmissionTime);
    }

    /**
     * Add registration specific information.
     * @param msg        adt message
     * @param storedFrom time that emap-core started processing the message.
     * @param visit      hospital visit to update
     */
    private void addRegistrationInformation(final RegisterPatient msg, Instant storedFrom, HospitalVisit visit) {
        try {
            if (msg.getPresentationDateTime().get().equals(visit.getPresentationTime())) {
                return;
            }
        } catch (IllegalStateException e) {
            logger.error("Registration message with no Presentation date time", e);
        }
        msg.getPresentationDateTime().assignTo(visit::setPresentationTime);
        updateStoredFromAndValidFrom(msg, storedFrom, visit);
    }

    /**
     * Save a newly created hospital visit, or the audit table for original visit if this has been updated.
     * @param visitState
     * @param originalVisit original visit
     */
    private void manuallySaveVisitOrAuditIfRequired(final RowState<HospitalVisit> visitState, final HospitalVisit originalVisit) {
        if (visitState.isEntityCreated()) {
            hospitalVisitRepo.save(visitState.getEntity());
            logger.debug("New HospitalVisit being saved: {}", visitState.getEntity());
        } else if (visitState.isEntityUpdated()) {
            AuditHospitalVisit audit = new AuditHospitalVisit(originalVisit, visitState.getMessageDateTime(), visitState.getStoredFrom());
            auditHospitalVisitRepo.save(audit);
            logger.debug("New AuditHospitalVisit being saved: {}", audit);
        }
    }

}
