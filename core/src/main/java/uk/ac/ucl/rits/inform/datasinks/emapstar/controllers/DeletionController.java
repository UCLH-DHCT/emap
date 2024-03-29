package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

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
    private final FormController formController;

    /**
     * Deletion controller needs access to other multiple controllers which each have their own delete methods.
     * @param pendingAdtController          controller for pending adt tables
     * @param labController                 controller for Lab tables
     * @param consultationRequestController controller for consultation request tables
     * @param hospitalVisitController       controller for visit tables
     * @param formController                controller for form tables
     */
    public DeletionController(
            PendingAdtController pendingAdtController, LabController labController,
            ConsultationRequestController consultationRequestController, VisitController hospitalVisitController, FormController formController) {
        this.pendingAdtController = pendingAdtController;
        this.labController = labController;
        this.consultationRequestController = consultationRequestController;
        this.hospitalVisitController = hospitalVisitController;
        this.formController = formController;
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
            formController.deleteFormsForVisit(visit, invalidationTime, deletionTime);
            hospitalVisitController.deleteVisit(visit, invalidationTime, deletionTime);
        }
    }

    /**
     * Deletes entities that are older than the current message, along with tables which require visits.
     * @param mrn              MRN of the patient to delete
     * @param invalidationTime Time of the delete information message
     * @param deletionTime     time that emap-core started processing the message.
     */
    public void deleteMrnDependentEntities(Mrn mrn, Instant invalidationTime, Instant deletionTime) {
        formController.deleteFormsForMrn(mrn, invalidationTime, deletionTime);
    }
}
