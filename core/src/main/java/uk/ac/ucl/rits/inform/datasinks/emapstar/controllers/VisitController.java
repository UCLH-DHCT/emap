package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditHospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

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
     */
    public HospitalVisit getOrCreateMinimalHospitalVisit(final String encounter, Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                                         final Instant storedFrom) {
        AtomicBoolean created = new AtomicBoolean(false);
        HospitalVisit visit = getOrCreateHospitalVisit(encounter, mrn, sourceSystem, messageDateTime, storedFrom, created);
        if (created.get()) {
            logger.debug("Minimal encounter created and saved. encounter: {}, mrn: {}", encounter, mrn);
            hospitalVisitRepo.save(visit);
        }
        return visit;
    }

    private HospitalVisit getOrCreateHospitalVisit(final String encounter, final Mrn mrn, final String sourceSystem,
                                                   final Instant messageDateTime, final Instant storedFrom, AtomicBoolean created) {
        return hospitalVisitRepo.findByEncounter(encounter)
                .orElseGet(() -> {
                    created.set(true);
                    return createHospitalVisit(encounter, mrn, sourceSystem, messageDateTime, storedFrom);
                });
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
    private HospitalVisit createHospitalVisit(final String encounter, Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                              final Instant storedFrom) {
        HospitalVisit visit = new HospitalVisit();
        visit.setMrnId(mrn);
        visit.setEncounter(encounter);
        visit.setSourceSystem(sourceSystem);
        visit.setStoredFrom(storedFrom);
        visit.setValidFrom(messageDateTime);
        return visit;
    }

    /**
     * Process information about hospital visits, saving any changes to the database.
     * @param msg             adt message
     * @param storedFrom      time that emap-core started processing the message.
     * @param messageDateTime date time of the message
     * @param mrn             mrn
     * @return hospital visit
     */
    @Transactional
    public HospitalVisit updateOrCreateHospitalVisit(AdtMessage msg, Instant storedFrom, Instant messageDateTime, Mrn mrn) {
        AtomicBoolean created = new AtomicBoolean(false);
        HospitalVisit visit = getOrCreateHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), msg.getRecordedDateTime(), storedFrom, created);
        final HospitalVisit originalVisit = visit.copy();
        if (messageShouldBeUpdated(messageDateTime, created, originalVisit)) {
            updateGenericData(msg, visit);
            // process message based on the class type
            if (msg instanceof AdmitPatient) {
                AdmitPatient admit = (AdmitPatient) msg;
                addAdmissionInformation(admit, storedFrom, visit);
            } else if (msg instanceof RegisterPatient) {
                RegisterPatient registerPatient = (RegisterPatient) msg;
                addRegistrationInformation(registerPatient, storedFrom, visit);

            }
            manuallySaveVisitOrAuditIfRequired(visit, originalVisit, created, messageDateTime, storedFrom);
        }
        return visit;
    }

    /**
     * If message is newer than the database, newly created or if the database has data from untrusted source.
     * @param messageDateTime date time of the message
     * @param created         has the visit just been created
     * @param originalVisit   original visit from
     * @return true if the message is newer or was created
     */
    private boolean messageShouldBeUpdated(final Instant messageDateTime, final AtomicBoolean created,
                                           final HospitalVisit originalVisit) {
        return originalVisit.getValidFrom().isBefore(messageDateTime) || created.get() || !originalVisit.getSourceSystem().equals("EPIC");
    }

    /**
     * Update visit with generic ADT information.
     * @param msg   adt message
     * @param visit hospital visit to update
     */
    public void updateGenericData(final AdtMessage msg, HospitalVisit visit) {
        msg.getPatientClass().assignTo(pc -> visit.setPatientClass(pc.toString()));
        msg.getModeOfArrival().assignTo(visit::setArrivalMethod);
        visit.setSourceSystem(msg.getSourceSystem());
    }

    private void updateStoredFromAndValidFrom(final AdtMessage msg, final Instant storedFrom, HospitalVisit visit) {
        visit.setValidFrom(msg.getRecordedDateTime());
        visit.setStoredFrom(storedFrom);
    }

    /**
     * Add admission specific information.
     * @param msg        adt message
     * @param storedFrom time that emap-core started processing the message.
     * @param visit      hospital visit to update
     */
    private void addAdmissionInformation(final AdmitPatient msg, Instant storedFrom, HospitalVisit visit) {
        try {
            if (msg.getAdmissionDateTime().get().equals(visit.getAdmissionTime())) {
                return;
            }
        } catch (IllegalStateException e) {
            logger.error("Admission message with no admission date time", e);
        }

        msg.getAdmissionDateTime().assignTo(visit::setAdmissionTime);
        updateStoredFromAndValidFrom(msg, storedFrom, visit);
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
     * @param visit           potentially updated visit
     * @param originalVisit   original visit
     * @param created         has the visit just been created
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     */
    private void manuallySaveVisitOrAuditIfRequired(final HospitalVisit visit, final HospitalVisit originalVisit, final AtomicBoolean created,
                                                   final Instant messageDateTime, final Instant storedFrom) {
        if (!visit.equals(originalVisit)) {
            if (created.get()) {
                hospitalVisitRepo.save(visit);
                logger.debug("New HospitalVisit being saved: {}", visit);
            } else {
                AuditHospitalVisit audit = new AuditHospitalVisit(originalVisit, messageDateTime, storedFrom);
                auditHospitalVisitRepo.save(audit);
                logger.debug("New AuditHospitalVisit being saved: {}", audit);
            }
        }
    }

}
