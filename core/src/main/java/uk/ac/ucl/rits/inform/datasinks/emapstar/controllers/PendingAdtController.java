package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovement;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovementAudit;
import uk.ac.ucl.rits.inform.interchange.adt.CancelPendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;

import java.time.Instant;

/**
 * Carries out the business logic part of pending ADT messages.
 * This could be added to the patient location controller, but it seemed big enough already.
 * When we have ADT edits (ADT^Z99) set up, and potentially the individual movement IDs in hl7 messages,
 * it may be worth thinking about the structure - hopefully less out-of-order processing would be required so the location visit should be simpler.
 * @author Stef Piatek
 */
@Component
public class PendingAdtController {
    private LocationController locationController;
    private PlannedMovementRepository plannedMovementRepo;
    private PlannedMovementAuditRepository plannedMovementAuditRepo;

    /**
     * @param locationController       To get the location entity for the planned move
     * @param plannedMovementRepo      To update the Planned Movement entity
     * @param plannedMovementAuditRepo To update the Audit log for Planned Movement
     */
    public PendingAdtController(LocationController locationController,
                                PlannedMovementRepository plannedMovementRepo,
                                PlannedMovementAuditRepository plannedMovementAuditRepo) {
        this.locationController = locationController;
        this.plannedMovementRepo = plannedMovementRepo;
        this.plannedMovementAuditRepo = plannedMovementAuditRepo;
    }


    /**
     * Process pending ADT requests.
     * @param visit      associated visit
     * @param msg        pending adt
     * @param validFrom  time in the hospital when the message was created
     * @param storedFrom time that emap core started processing the message
     */
    public void processMsg(HospitalVisit visit, PendingTransfer msg, Instant validFrom, Instant storedFrom) {
        Location plannedLocation = null;
        if (msg.getPendingLocation().isSave()) {
            plannedLocation = locationController.getOrCreateLocation(msg.getPendingLocation().get());
        }

        var plannedState = getOrCreateFromPlannedMessage(
                visit, plannedLocation, msg.getPendingEventType().toString(), msg.getEventOccurredDateTime(), validFrom, storedFrom
        );
        PlannedMovement plannedMovement = plannedState.getEntity();
        // If we receive a cancelled message before the original request then add it in
        if (!plannedState.isEntityCreated() && plannedMovement.getEventDatetime() == null) {
            plannedState.assignIfDifferent(plannedMovement.getEventDatetime(), msg.getEventOccurredDateTime(), plannedMovement::setEventDatetime);
        }

        plannedState.saveEntityOrAuditLogIfRequired(plannedMovementRepo, plannedMovementAuditRepo);
    }

    private RowState<PlannedMovement, PlannedMovementAudit> getOrCreateFromPlannedMessage(
            HospitalVisit visit, Location plannedLocation, String pendingEventType, Instant eventOccurredDateTime,
            Instant validFrom, Instant storedFrom) {
        return plannedMovementRepo.findFirstFromPlannedMovement(pendingEventType, visit, plannedLocation, eventOccurredDateTime)
                .map(pm -> new RowState<>(pm, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    PlannedMovement pm = new PlannedMovement(visit, plannedLocation, pendingEventType);
                    pm.setEventDatetime(eventOccurredDateTime);
                    return new RowState<>(pm, validFrom, storedFrom, true);
                });
    }

    public void processMsg(HospitalVisit visit, CancelPendingTransfer msg, Instant validFrom, Instant storedFrom) {
        return;
    }
}
