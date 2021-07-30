package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.TemporalFrom;

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

    private Boolean isBunk;

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

    public BedState(Bed bedId, Long csn, Boolean isInCensus, Boolean isBunk, String status, Boolean isPool, TemporalFrom temporalFrom) {
        this.bedId = bedId;
        this.csn = csn;
        this.isInCensus = isInCensus;
        this.isBunk = isBunk;
        this.status = status;
        if (isPool) {
            // will increment pool bed upon updating so initialise as 0 only if it's a pool bed
            poolBedCount = 0L;
        }
        this.validFrom = temporalFrom.getValid();
        this.storedFrom = temporalFrom.getStored();
    }

    public void incrementPoolBedCount() {
        poolBedCount += 1;
    }
}
