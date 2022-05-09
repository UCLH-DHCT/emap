package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * \brief Tracks the final history for each planned movement within the hospital.
 * <p>
 * Each
 * @author Stef Piatek
 */
@Entity
@Table
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@AuditTable
public class PlannedMovement extends TemporalCore<PlannedMovement, PlannedMovementAudit> {
    /**
     * \brief Unique identifier in EMAP for this PlannedMovement record.
     * <p>
     * This is the primary key for the PlannedMovement table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long plannedMovementId;

    /**
     * \brief Identifier for the HospitalVisit associated with this record.
     * <p>
     * This is a foreign key that joins the locationVisit table to the HospitalVisit table.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /**
     * \brief Planned Location to move to, may be null.
     */
    @ManyToOne
    @JoinColumn(name = "locationId")
    private Location locationId;

    /**
     * /brief The type of planned movement event (ADMIT, TRANSFER, DISCHARGE).
     */
    private String eventType;

    /**
     * /brief The date and time that the planned movement event was made.
     */
    private Instant eventDatetime;

    /**
     * /brief Has the planned movement been cancelled (either by a user or because a different movement has occurred).
     */
    private Boolean cancelled;

    /**
     * /brief The date and time that the planned movement was cancelled.
     */
    private Instant cancelledDatetime;

    /**
     * Minimal constructor.
     * @param hospitalVisitId associated hospital visit
     * @param locationId      planned location, may be null
     * @param eventType       type of movement event
     */
    public PlannedMovement(HospitalVisit hospitalVisitId, Location locationId, String eventType) {
        this.hospitalVisitId = hospitalVisitId;
        this.locationId = locationId;
        this.eventType = eventType;
    }

    /**
     * Copy constructor.
     * @param other entity to copy
     */
    private PlannedMovement(PlannedMovement other) {
        super(other);
        plannedMovementId = other.plannedMovementId;
        hospitalVisitId = other.hospitalVisitId;
        locationId = other.locationId;
        eventType = other.eventType;
        eventDatetime = other.eventDatetime;
        cancelled = other.cancelled;
        cancelledDatetime = other.cancelledDatetime;
    }

    @Override
    public PlannedMovement copy() {
        return new PlannedMovement(this);
    }

    @Override
    public PlannedMovementAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new PlannedMovementAudit(this, validUntil, storedUntil);
    }
}
