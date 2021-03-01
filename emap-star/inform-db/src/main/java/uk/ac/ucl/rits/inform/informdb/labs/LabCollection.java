package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * A LabCollection details the collection of the sample being analysed and its
 * receipt by the lab system. There may be more than one collection per
 * LabNumber.
 * @author Roma Klapaukh
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
@Table
public class LabCollection extends TemporalCore<LabCollection, LabCollectionAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labCollectionId;

    @ManyToOne
    @JoinColumn(name = "labNumberId", nullable = false)
    private LabNumber labNumberId;

    /**
     * The time the sample arrived at the lab where there test was being performed.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant receiptAtLab;

    /**
     * The time the sample was take from the patient (e.g. time of phlebotomy).
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant sampleCollectionTime;

    /**
     * Type of specimen.
     * E.g. Mid Stream Urine
     */
    private String specimenType;
    /**
     * Site the sample was taken from.
     * E.g. Right kidney
     */
    private String sampleSite;

    public LabCollection() {}

    public LabCollection(LabNumber labNumberId, Instant validFrom, Instant storedFrom) {
        this.labNumberId = labNumberId;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    public LabCollection(LabCollection other) {
        super(other);
        this.labCollectionId = other.labCollectionId;
        this.labNumberId = other.labNumberId;
        this.receiptAtLab = other.receiptAtLab;
        this.sampleCollectionTime = other.sampleCollectionTime;
        this.specimenType = other.specimenType;
    }

    @Override
    public LabCollection copy() {
        return new LabCollection(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public LabCollectionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabCollectionAudit(this, validUntil, storedUntil);
    }
}
