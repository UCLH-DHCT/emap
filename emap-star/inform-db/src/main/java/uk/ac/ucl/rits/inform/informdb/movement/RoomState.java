package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;


@SuppressWarnings("serial")
@Entity
@Table
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoomState extends AuditCore<RoomState> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long roomStateId;

    @ManyToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room roomId;

    @Column(nullable = false, unique = true)
    private Long csn;

    private String status;

    private Boolean isReady;

    /**
     * Constructor for an active room state.
     * @param roomId     room entity
     * @param csn        room CSN
     * @param status     current room status
     * @param isReady    is the room ready
     * @param validFrom  time that the room was first used as a contact
     * @param storedFrom time that star started processing the message
     */
    public RoomState(Room roomId, Long csn, String status, Boolean isReady, Instant validFrom, Instant storedFrom) {
        this.roomId = roomId;
        this.csn = csn;
        this.status = status;
        this.isReady = isReady;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    private RoomState(RoomState other) {
        super(other);
        setValidFrom(other.getValidFrom());
        setStoredFrom(other.getValidUntil());
        roomId = other.roomId;
        status = other.status;
        isReady = other.isReady;
    }

    @Override
    public RoomState copy() {
        return new RoomState(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public RoomState createAuditEntity(Instant validUntil, Instant storedUntil) {
        RoomState audit = copy();
        audit.setValidUntil(validUntil);
        audit.setStoredUntil(storedUntil);
        return audit;
    }
}
