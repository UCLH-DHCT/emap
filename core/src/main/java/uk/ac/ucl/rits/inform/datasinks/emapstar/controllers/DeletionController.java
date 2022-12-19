package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import java.time.Instant;

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
    private final PendingAdtController pendingAdtController;
    private final LabController labController;
    private final ConsultationRequestController consultationRequestController;
    private final VisitController hospitalVisitController;

    /**
     * Deletion controller needs access to other multiple controllers which each have their own delete methods.
     *
     * @param pendingAdtController              controller for pending adt tables
     * @param labController                     controller for Lab tables
     * @param consultationRequestController     controller for consultation request tables
     * @param hospitalVisitController           controller for visit tables
     */
    public DeletionController(
            PendingAdtController pendingAdtController, LabController labController,
            ConsultationRequestController consultationRequestController, VisitController hospitalVisitController) {
        this.pendingAdtController = pendingAdtController;
        this.labController = labController;
        this.consultationRequestController = consultationRequestController;
        this.hospitalVisitController = hospitalVisitController;
    }

    /**
     * Deletes visits that are older than the current message, along with tables which require visits.
     * @param visits           List of hospital visits
     * @param invalidationTime Time of the delete information message
     * @param deletionTime     time that emap-core started processing the message.
     */
    public void deleteVisitsAndDependentEntities(Iterable<HospitalVisit> visits, Instant invalidationTime, Instant deletionTime) {
        for (HospitalVisit visit : visits) {
            pendingAdtController.deletePlannedMovements(visit, invalidationTime, deletionTime);
            labController.deleteLabOrdersForVisit(visit, invalidationTime, deletionTime);
            consultationRequestController.deleteConsultRequestsForVisit(visit, invalidationTime, deletionTime);
            hospitalVisitController.deleteVisit(visit, invalidationTime, deletionTime);
        }
    }

    private void deleteFormsForVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        List<Form> allFormsForVisit = formRepository.findAllByHospitalVisitId(visit);
        deleteForms(allFormsForVisit, invalidationTime, deletionTime);
    }

    /**
     * Delete all forms and form answers directly attached to an MRN.
     * @param mrn mrn to delete from
     * @param invalidationTime invalidation time
     * @param deletionTime deletion time
     */
    public void deleteFormsForMrn(Mrn mrn, Instant invalidationTime, Instant deletionTime) {
        List<Form> allFormsForMrn = formRepository.findAllByMrnId(mrn);
        deleteForms(allFormsForMrn, invalidationTime, deletionTime);
    }


    private void deleteForms(List<Form> allFormsForVisit, Instant invalidationTime, Instant deletionTime) {
        for (Form form : allFormsForVisit) {
            List<FormAnswer> formAnswers = form.getFormAnswers();
            for (FormAnswer ans : formAnswers) {
                formAnswerAuditRepository.save(ans.createAuditEntity(invalidationTime, deletionTime));
            }
            formAnswerRepository.deleteAll(formAnswers);
            formAuditRepository.save(form.createAuditEntity(invalidationTime, deletionTime));
        }
        formRepository.deleteAll(allFormsForVisit);
    }

    /**
     * Delete planned movements from a delete patient information message.
     * @param visit            Hospital visit that should have their planned movements deleted
     * @param invalidationTime Time of the delete information message
     * @param deletionTime     time that emap-core started processing the message.
     */
    private void deletePlannedMovements(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        plannedMovementRepo.findAllByHospitalVisitId(visit)
                .forEach(plannedMovement -> deletePlannedMovement(plannedMovement, invalidationTime, deletionTime));
    }

    /**
     * Audit and delete a planned movement.
     * @param plannedMovement Planned movement to delete
     * @param deletionTime    Hospital time that the planned movement was deleted at
     * @param storedUntil     Time that emap-core started processing the message.
     */
    private void deletePlannedMovement(PlannedMovement plannedMovement, Instant deletionTime, Instant storedUntil) {
        plannedMovementAuditRepo.save(plannedMovement.createAuditEntity(deletionTime, storedUntil));
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
