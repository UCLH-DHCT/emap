package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;


@SuppressWarnings("serial")
@Entity
@Table
@Data
@NoArgsConstructor
public class RoomState implements Serializable {
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

    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant validFrom;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;

    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant storedFrom;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;

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
        this.validFrom = validFrom;
        this.storedFrom = storedFrom;
    }
}
