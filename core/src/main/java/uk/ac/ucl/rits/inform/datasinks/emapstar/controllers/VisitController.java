package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditHospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

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

    public HospitalVisit getOrCreateHospitalVisit(final String encounter, Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                                  final Instant storedFrom, final boolean isFromAdt) {
        return hospitalVisitRepo.findByEncounter(encounter)
                .orElseGet(() -> createHospitalVisit(encounter, mrn, sourceSystem, messageDateTime, storedFrom, isFromAdt));
    }

    /**
     * Update visit with adt message information if it is from an untrusted source or the encounter has just been created from an adt message.
     * @param adtMessage      nullable adt message
     * @param messageDateTime message date time
     * @param storedFrom      date time the message was processed by emap core
     * @param visit           hospital visit
     * @return audit entity if the visit wasn't just created.
     */
    public AuditHospitalVisit updateVisitIfUntrustedSystemOrNewlyCreated(final AdtMessage adtMessage, final Instant messageDateTime, final Instant storedFrom,
                                                                         HospitalVisit visit) {
        AuditHospitalVisit audit = null;
        if (!visit.getSourceSystem().equals("EPIC")) {
            if (!visit.getSourceSystem().isEmpty()) {
                // save current state to audit table if this is not a newly created adt message
                audit = new AuditHospitalVisit(visit, messageDateTime, storedFrom);
            }
            addAdtInformation(adtMessage, storedFrom, visit);
        }
        return audit;
    }

    private HospitalVisit createHospitalVisit(final String encounter, Mrn mrn, final String sourceSystem, final Instant messageDateTime,
                                              final Instant storedFrom, final boolean isFromAdt) {
        HospitalVisit visit = new HospitalVisit();
        visit.setMrnId(mrn);
        visit.setEncounter(encounter);
        // Adt encounter should be further processed before saving
        if (!isFromAdt) {
            visit.setSourceSystem(sourceSystem);
            visit.setStoredFrom(storedFrom);
            visit.setValidFrom(messageDateTime);
            hospitalVisitRepo.save(visit);
        }
        return visit;
    }

    private void addAdtInformation(final AdtMessage adtMessage, final Instant storedFrom,
                                   HospitalVisit hospitalVisit) {
        adtMessage.getPatientClass().assignTo(pc -> hospitalVisit.setPatientClass(pc.toString()));
        adtMessage.getModeOfArrival().assignTo(hospitalVisit::setArrivalMethod);
        // update source system
        hospitalVisit.setSourceSystem(adtMessage.getSourceSystem());
        hospitalVisit.setStoredFrom(storedFrom);
        hospitalVisit.setValidFrom(adtMessage.getRecordedDateTime());
    }

    /**
     * Save audit hospital visit.
     * @param audit Audit hospital visit
     */
    @Transactional
    public void saveAuditIfExists(@Nullable final AuditHospitalVisit audit) {
        if (audit != null) {
            auditHospitalVisitRepo.save(audit);
        }
    }
}
