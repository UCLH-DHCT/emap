package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestAudit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisitAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovement;

import java.time.Instant;
import java.util.List;

/**
 * @author Jeremy Stein
 * <p>
 * Operations to delete entities in a cascading fashion, creating new audit rows as appropriate.
 * This was written with performing unfiltered, cascading deletes in mind
 * eg. delete an entire person's record.
 * It could be adapted to be more selective (but still be cascading).
 */
@Component
public class DeletionController {
    private final LabOrderRepository labOrderRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;
    private final LabResultRepository labResultRepo;
    private final LabResultAuditRepository labResultAuditRepo;
    private final ConsultationRequestRepository consultationRequestRepo;
    private final ConsultationRequestAuditRepository consultationRequestAuditRepo;
    private final HospitalVisitRepository hospitalVisitRepo;
    private final HospitalVisitAuditRepository hospitalVisitAuditRepo;
    private final PlannedMovementRepository plannedMovementRepo;
    private final PlannedMovementAuditRepository plannedMovementAuditRepo;

    /**
     * Deletion controller needs access to pretty much every repo in order to do cascading deletes.
     * @param labOrderRepo                 lab order repo
     * @param labOrderAuditRepo            lab order audit repo
     * @param labResultRepo                lab result repo
     * @param labResultAuditRepo           lab result audit repo
     * @param consultationRequestRepo      consultation request repo
     * @param consultationRequestAuditRepo consultation request audit repo
     * @param hospitalVisitRepo            hospital visit repo
     * @param hospitalVisitAuditRepo       hospital visit audit repo
     * @param plannedMovementRepo          planned movement repo
     * @param plannedMovementAuditRepo     planned movement audit repo
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public DeletionController(
            LabOrderRepository labOrderRepo, LabOrderAuditRepository labOrderAuditRepo,
            LabResultRepository labResultRepo, LabResultAuditRepository labResultAuditRepo,
            ConsultationRequestRepository consultationRequestRepo, ConsultationRequestAuditRepository consultationRequestAuditRepo,
            HospitalVisitRepository hospitalVisitRepo, HospitalVisitAuditRepository hospitalVisitAuditRepo,
            PlannedMovementRepository plannedMovementRepo, PlannedMovementAuditRepository plannedMovementAuditRepo
    ) {
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
        this.consultationRequestRepo = consultationRequestRepo;
        this.consultationRequestAuditRepo = consultationRequestAuditRepo;
        this.hospitalVisitRepo = hospitalVisitRepo;
        this.hospitalVisitAuditRepo = hospitalVisitAuditRepo;
        this.plannedMovementRepo = plannedMovementRepo;
        this.plannedMovementAuditRepo = plannedMovementAuditRepo;
    }

    /**
     * Delete all visits that are older than the current message, along with tables which require visits.
     * @param visits           List of hopsital visits
     * @param invalidationTime Time of the delete information message
     * @param deletionTime     time that emap-core started processing the message.
     */
    public void deleteVisitsAndDependentEntities(Iterable<HospitalVisit> visits, Instant invalidationTime, Instant deletionTime) {
        for (HospitalVisit visit : visits) {
            deletePlannedMovements(visit, invalidationTime, deletionTime);
            deleteLabOrdersForVisit(visit, invalidationTime, deletionTime);
            deleteConsultRequestsForVisit(visit, invalidationTime, deletionTime);

            hospitalVisitAuditRepo.save(visit.createAuditEntity(invalidationTime, deletionTime));
            hospitalVisitRepo.delete(visit);
        }
    }

    /**
     * Delete planned movements from a delete patient information message.
     * @param visit            Hospital visit that should have their planned movements deleted
     * @param invalidationTime Time of the delete information message
     * @param deletionTime     time that emap-core started processing the message.
     */
    private void deletePlannedMovements(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        plannedMovementRepo.findAllByHospitalVisitId(visit)
                .forEach(plannedMovement -> deletePlannedMovement(invalidationTime, deletionTime, plannedMovement));
    }

    private void deletePlannedMovement(Instant validUntil, Instant storedUntil, PlannedMovement plannedMovement) {
        plannedMovementAuditRepo.save(plannedMovement.createAuditEntity(validUntil, storedUntil));
        plannedMovementRepo.delete(plannedMovement);
    }

    private void deleteLabOrdersForVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        List<LabOrder> labOrders = labOrderRepo.findAllByHospitalVisitId(visit);
        for (var lo : labOrders) {
            deleteLabResultsForLabOrder(lo, invalidationTime, deletionTime);

            LabOrderAudit labOrderAudit = lo.createAuditEntity(invalidationTime, deletionTime);
            labOrderAuditRepo.save(labOrderAudit);
            labOrderRepo.delete(lo);
        }

    }

    private void deleteLabResultsForLabOrder(LabOrder labOrder, Instant invalidationTime, Instant deletionTime) {
        List<LabResult> labResults = labResultRepo.findAllByLabOrderId(labOrder);
        for (var lr : labResults) {
            LabResultAudit resultAudit = lr.createAuditEntity(invalidationTime, deletionTime);
            labResultAuditRepo.save(resultAudit);
            labResultRepo.delete(lr);
        }
    }

    private void deleteConsultRequestsForVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        List<ConsultationRequest> consultationRequests = consultationRequestRepo.findAllByHospitalVisitId(visit);
        for (var cr : consultationRequests) {
            ConsultationRequestAudit auditEntity = cr.createAuditEntity(invalidationTime, deletionTime);
            consultationRequestAuditRepo.save(auditEntity);
            consultationRequestRepo.delete(cr);
        }
    }

}
