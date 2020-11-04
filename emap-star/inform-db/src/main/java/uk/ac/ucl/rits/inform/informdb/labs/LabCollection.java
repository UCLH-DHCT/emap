package uk.ac.ucl.rits.inform.informdb.labs;

import uk.ac.ucl.rits.inform.informdb.AuditCore;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

/**
 * A LabCollection details the collection of the sample being analysed and its
 * receipt by the lab system. There may be more than one collection per
 * LabNumber.
 * @author Roma Klapaukh
 */
@Entity
public class LabCollection extends TemporalCore<LabCollection, AuditCore> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labCollectionId;

    private long labCollectionDurableId;
    private long labNumberId;

    /**
     * The time the sample arrived at the lab where there test was being performed.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant sampleReceiptTime;

    /**
     * The time the sample was take from the patient (e.g. time of phlebotomy).
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant sampleCollectionTime;

    private String sampleType;

    public LabCollection() {}

    public LabCollection(LabCollection other) {
        super(other);

        this.labCollectionDurableId = other.labCollectionDurableId;
        this.labNumberId = other.labNumberId;

        this.sampleReceiptTime = other.sampleReceiptTime;
        this.sampleCollectionTime = other.sampleCollectionTime;

        this.sampleType = other.sampleType;
    }

    /**
     * @return the labCollectionId
     */
    public long getLabCollectionId() {
        return labCollectionId;
    }

    /**
     * @param labCollectionId the labCollectionId to set
     */
    public void setLabCollectionId(long labCollectionId) {
        this.labCollectionId = labCollectionId;
    }

    /**
     * @return the labCollectionDurableId
     */
    public long getLabCollectionDurableId() {
        return labCollectionDurableId;
    }

    /**
     * @param labCollectionDurableId the labCollectionDurableId to set
     */
    public void setLabCollectionDurableId(long labCollectionDurableId) {
        this.labCollectionDurableId = labCollectionDurableId;
    }

    /**
     * @return the labNumberId
     */
    public long getLabNumberId() {
        return labNumberId;
    }

    /**
     * @param labNumberId the labNumberId to set
     */
    public void setLabNumberId(long labNumberId) {
        this.labNumberId = labNumberId;
    }

    /**
     * @return the sampleReceiptTime
     */
    public Instant getSampleReceiptTime() {
        return sampleReceiptTime;
    }

    /**
     * @param sampleReceiptTime the sampleReceiptTime to set
     */
    public void setSampleReceiptTime(Instant sampleReceiptTime) {
        this.sampleReceiptTime = sampleReceiptTime;
    }

    /**
     * @return the sampleCollectionTime
     */
    public Instant getSampleCollectionTime() {
        return sampleCollectionTime;
    }

    /**
     * @param sampleCollectionTime the sampleCollectionTime to set
     */
    public void setSampleCollectionTime(Instant sampleCollectionTime) {
        this.sampleCollectionTime = sampleCollectionTime;
    }

    /**
     * @return the sampleType
     */
    public String getSampleType() {
        return sampleType;
    }

    /**
     * @param sampleType the sampleType to set
     */
    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    @Override
    public LabCollection copy() {
        return new LabCollection(this);
    }

    /**
     * @param validUntil the event time that invalidated the current state
     * @param storedFrom the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public AuditCore createAuditEntity(Instant validUntil, Instant storedFrom) {
        throw new UnsupportedOperationException();
    }
}
