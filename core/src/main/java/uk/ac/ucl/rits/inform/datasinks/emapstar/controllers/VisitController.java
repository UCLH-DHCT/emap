package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditHospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

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
     * Get or create hospital visit from non-adt source.
     * Will create a minimum hospital visit and save it it can't match one by the encounter string.
     * @param encounter       encounter number
     * @param mrn             Mrn
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return hospital visit
     */
    public HospitalVisit getOrCreateNonAdtHospitalVisit(final String encounter, Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                                        final Instant storedFrom) {
        AtomicBoolean created = new AtomicBoolean(false);
        HospitalVisit visit = getOrCreateHospitalVisit(encounter, mrn, sourceSystem, messageDateTime, storedFrom, created);
        if (created.get()) {
            hospitalVisitRepo.save(visit);
        }
        return visit;
    }

    /**
     * Note: does not save the entity if created as further processing is expected.
     * @param mrn        MRN
     * @param adtMessage ADT message
     * @param storedFrom when the message has been read by emap core
     * @param created    initial value for whether an entity was created
     * @return Pair of: existing, updated or created hospital visit; original state of the visit after get or create
     */
    @Transactional
    public Pair<HospitalVisit, HospitalVisit> getCreateOrUpdateHospitalVisit(final Mrn mrn, final AdtMessage adtMessage,
                                                                             final Instant storedFrom, AtomicBoolean created) {
        HospitalVisit visit = getOrCreateHospitalVisit(
                adtMessage.getVisitNumber(), mrn, adtMessage.getSourceSystem(), adtMessage.getRecordedDateTime(), storedFrom, created);
        HospitalVisit originalVisit = visit.copy();
        updateGenericAdtDataIfRequired(adtMessage, created, visit);
        return new MutablePair<>(visit, originalVisit);
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
     * Update visit with adt message information if it is from an untrusted source or the encounter has just been created.
     * @param adtMessage nullable adt message
     * @param created    has the visit just been created
     * @param visit      hospital visit to update
     */
    public void updateGenericAdtDataIfRequired(final AdtMessage adtMessage, final AtomicBoolean created, HospitalVisit visit) {
        if (created.get() || !visit.getSourceSystem().equals("EPIC")) {
            adtMessage.getPatientClass().assignTo(pc -> visit.setPatientClass(pc.toString()));
            adtMessage.getModeOfArrival().assignTo(visit::setArrivalMethod);
            adtMessage.setSourceSystem(adtMessage.getSourceSystem());
        }
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
     * Save a newly created hospital visit, or the audit table for original visit if this has been updated.
     * @param visitAndOriginalState visit after processing and the original state after get or create
     * @param created               has the visit just been created
     * @param messageDateTime       date time of the message
     * @param storedFrom            when the message has been read by emap core
     */
    @Transactional
    public void manuallySaveVisitOrAuditIfRequired(final Pair<HospitalVisit, HospitalVisit> visitAndOriginalState, final AtomicBoolean created,
                                                   final Instant messageDateTime, final Instant storedFrom) {
        if (originalAndVisitStateAreDifferent(visitAndOriginalState)) {
            if (created.get()) {
                hospitalVisitRepo.save(visitAndOriginalState.getLeft());
            } else {
                AuditHospitalVisit audit = new AuditHospitalVisit(visitAndOriginalState.getRight(), messageDateTime, storedFrom);
                auditHospitalVisitRepo.save(audit);
            }
        }
    }

    /**
     * @param visitAndOriginalState visit after processing and the original state after get or create
     * @return true if the original state has been changed
     */
    private boolean originalAndVisitStateAreDifferent(Pair<HospitalVisit, HospitalVisit> visitAndOriginalState) {
        return !visitAndOriginalState.getLeft().equals(visitAndOriginalState.getRight());
    }
}
