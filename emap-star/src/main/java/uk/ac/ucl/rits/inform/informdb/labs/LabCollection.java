package uk.ac.ucl.rits.inform.informdb.labs;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * A LabCollection details the collection of the sample being analysed and its
 * receipt by the lab system. There may be more than one collection per LabNumber.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class LabCollection extends TemporalCore<LabCollection> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long    labCollectionId;

    private long    labCollectionDurableId;
    private long    labNumberId;

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

    private String  sampleType;

    public LabCollection() {}

    public LabCollection(LabCollection other) {
        super(other);

        this.labCollectionDurableId = other.labCollectionDurableId;
        this.labNumberId = other.labNumberId;

        this.sampleReceiptTime = other.sampleReceiptTime;
        this.sampleCollectionTime = other.sampleCollectionTime;

        this.sampleType = other.sampleType;
    }

    @Override
    public LabCollection copy() {
        return new LabCollection(this);
    }
}
