package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

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
 * A LabSample details the external lab's view of a sample being analysed and its receipt by the lab system.
 * @author Roma Klapaukh
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
@Table
public class LabSample extends TemporalCore<LabSample, LabSampleAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labSampleId;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    /**
     * Lab number for the system doing the lab test.
     */
    private String externalLabNumber;
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

    public LabSample() {}

    public LabSample(Mrn mrnId, String externalLabNumber, Instant validFrom, Instant storedFrom) {
        this.mrnId = mrnId;
        this.externalLabNumber = externalLabNumber;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    public LabSample(LabSample other) {
        super(other);
        this.mrnId = other.mrnId;
        this.labSampleId = other.labSampleId;
        this.externalLabNumber = other.externalLabNumber;
        this.receiptAtLab = other.receiptAtLab;
        this.sampleCollectionTime = other.sampleCollectionTime;
        this.specimenType = other.specimenType;
    }

    @Override
    public LabSample copy() {
        return new LabSample(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public LabSampleAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabSampleAudit(this, validUntil, storedUntil);
    }
}
