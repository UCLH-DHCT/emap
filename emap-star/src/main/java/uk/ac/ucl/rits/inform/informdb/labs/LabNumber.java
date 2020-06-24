package uk.ac.ucl.rits.inform.informdb.labs;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * A lab number represents a reference number attached to a lab. As labs are
 * often done externally, there is separate fields to track the lab number from
 * the point of view of the EHR compared to the system processing the lab.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class LabNumber implements Serializable {

    private static final long serialVersionUID = -5771782759320217911L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long              labNumberId;

    private long              mrnId;
    private long              hospitalVisitDurableId;

    /**
     * Lab number in the EHR.
     */
    private String            internalLabNumber;
    /**
     * Lab number for the system doing the lab test.
     */
    private String            externalLabNumber;
    private String            sourceSystem;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant           storedFrom;

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
     * @return the mrnId
     */
    public long getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(long mrnId) {
        this.mrnId = mrnId;
    }

    /**
     * @return the hospitalVisitDurableId
     */
    public long getHospitalVisitDurableId() {
        return hospitalVisitDurableId;
    }

    /**
     * @param hospitalVisitDurableId the hospitalVisitDurableId to set
     */
    public void setHospitalVisitDurableId(long hospitalVisitDurableId) {
        this.hospitalVisitDurableId = hospitalVisitDurableId;
    }

    /**
     * @return the internalLabNumber
     */
    public String getInternalLabNumber() {
        return internalLabNumber;
    }

    /**
     * @param internalLabNumber the internalLabNumber to set
     */
    public void setInternalLabNumber(String internalLabNumber) {
        this.internalLabNumber = internalLabNumber;
    }

    /**
     * @return the externalLabNumber
     */
    public String getExternalLabNumber() {
        return externalLabNumber;
    }

    /**
     * @param externalLabNumber the externalLabNumber to set
     */
    public void setExternalLabNumber(String externalLabNumber) {
        this.externalLabNumber = externalLabNumber;
    }

    /**
     * @return the sourceSystem
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem the sourceSystem to set
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * @return the storedFrom
     */
    public Instant getStoredFrom() {
        return storedFrom;
    }

    /**
     * @param storedFrom the storedFrom to set
     */
    public void setStoredFrom(Instant storedFrom) {
        this.storedFrom = storedFrom;
    }

}
