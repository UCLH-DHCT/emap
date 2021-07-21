package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;

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
public class BedState implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bedStateId;

    @ManyToOne
    @JoinColumn(name = "bedId", nullable = false)
    private Bed bedId;

    @Column(nullable = false, unique = true)
    private Long csn;

    private Boolean isInCensus;

    private String status;

    private Long poolBedCount;

    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant validFrom;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;

    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant storedFrom;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;

}
