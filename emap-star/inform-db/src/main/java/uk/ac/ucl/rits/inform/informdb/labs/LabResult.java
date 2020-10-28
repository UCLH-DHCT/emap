package uk.ac.ucl.rits.inform.informdb.labs;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * A LabResult is a single component result of a lab. A single order or sample
 * is likely to produce several results.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class LabResult extends TemporalCore<LabResult> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long    labResultId;
    private long    labResultDurableId;

    private long    labNumberId;
    private long    labTestDefinitionDurableId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant resultLastModifiedTime;

    private boolean abnormal;
    private String  resultAsText;
    private double  resultAsReal;

    private String  resultOperator;
    private double  rangeHigh;
    private double  rangeLow;

    private String  comment;

    public LabResult() {}

    public LabResult(LabResult other) {
        super(other);

        this.labResultDurableId = other.labResultDurableId;
        this.labNumberId = other.labNumberId;
        this.labTestDefinitionDurableId = other.labTestDefinitionDurableId;
        this.resultLastModifiedTime = other.resultLastModifiedTime;
        this.abnormal = other.abnormal;
        this.resultAsText = other.resultAsText;
        this.resultAsReal = other.resultAsReal;
        this.resultOperator = other.resultOperator;
        this.rangeHigh = other.rangeHigh;
        this.rangeLow = other.rangeLow;
        this.comment = other.comment;
    }

    /**
     * @return the labResultId
     */
    public long getLabResultId() {
        return labResultId;
    }

    /**
     * @param labResultId the labResultId to set
     */
    public void setLabResultId(long labResultId) {
        this.labResultId = labResultId;
    }

    /**
     * @return the labResultDurableId
     */
    public long getLabResultDurableId() {
        return labResultDurableId;
    }

    /**
     * @param labResultDurableId the labResultDurableId to set
     */
    public void setLabResultDurableId(long labResultDurableId) {
        this.labResultDurableId = labResultDurableId;
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
     * @return the labTestDefinitionDurableId
     */
    public long getLabTestDefinitionDurableId() {
        return labTestDefinitionDurableId;
    }

    /**
     * @param labTestDefinitionDurableId the labTestDefinitionDurableId to set
     */
    public void setLabTestDefinitionDurableId(long labTestDefinitionDurableId) {
        this.labTestDefinitionDurableId = labTestDefinitionDurableId;
    }

    /**
     * @return the resultLastModifiedTime
     */
    public Instant getResultLastModifiedTime() {
        return resultLastModifiedTime;
    }

    /**
     * @param resultLastModifiedTime the resultLastModifiedTime to set
     */
    public void setResultLastModifiedTime(Instant resultLastModifiedTime) {
        this.resultLastModifiedTime = resultLastModifiedTime;
    }

    /**
     * @return the abnormal
     */
    public boolean isAbnormal() {
        return abnormal;
    }

    /**
     * @param abnormal the abnormal to set
     */
    public void setAbnormal(boolean abnormal) {
        this.abnormal = abnormal;
    }

    /**
     * @return the resultAsText
     */
    public String getResultAsText() {
        return resultAsText;
    }

    /**
     * @param resultAsText the resultAsText to set
     */
    public void setResultAsText(String resultAsText) {
        this.resultAsText = resultAsText;
    }

    /**
     * @return the resultAsReal
     */
    public double getResultAsReal() {
        return resultAsReal;
    }

    /**
     * @param resultAsReal the resultAsReal to set
     */
    public void setResultAsReal(double resultAsReal) {
        this.resultAsReal = resultAsReal;
    }

    /**
     * @return the resultOperator
     */
    public String getResultOperator() {
        return resultOperator;
    }

    /**
     * @param resultOperator the resultOperator to set
     */
    public void setResultOperator(String resultOperator) {
        this.resultOperator = resultOperator;
    }

    /**
     * @return the rangeHigh
     */
    public double getRangeHigh() {
        return rangeHigh;
    }

    /**
     * @param rangeHigh the rangeHigh to set
     */
    public void setRangeHigh(double rangeHigh) {
        this.rangeHigh = rangeHigh;
    }

    /**
     * @return the rangeLow
     */
    public double getRangeLow() {
        return rangeLow;
    }

    /**
     * @param rangeLow the rangeLow to set
     */
    public void setRangeLow(double rangeLow) {
        this.rangeLow = rangeLow;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public LabResult copy() {
        return new LabResult(this);
    }

}
