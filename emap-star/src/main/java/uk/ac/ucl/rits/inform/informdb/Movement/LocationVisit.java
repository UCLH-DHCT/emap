package uk.ac.ucl.rits.inform.informdb.Movement;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * This represents a patient being in a location for an amount of time. Every
 * location visit is part of a hospital visit, as you have to be in the hospital
 * before you can go to a specific location within it. Location visits can
 * optionally have a parent location visit. This happens when the patient is
 * still considered to be at the parent location (e.g. going down to an MRI
 * scanner from a ward bed doesn't vacate the ward bed).
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties({})
@Table(indexes = {})
public class LocationVisit extends TemporalCore implements Serializable {

    private static final long serialVersionUID = -8228844390430073225L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long              bedVisitId;
    @Column(nullable = false)
    private long              bedVisitDurableIid;

    @Column(nullable = false)
    private long              parentHospitalVisitDurableId;
    private long              parentBedVisitDurableId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant           admissionTime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           dischargeTime;
    private String            location;

    /**
     * @return the bedVisitId
     */
    public long getBedVisitId() {
        return bedVisitId;
    }

    /**
     * @param bedVisitId the bedVisitId to set
     */
    public void setBedVisitId(long bedVisitId) {
        this.bedVisitId = bedVisitId;
    }

    /**
     * @return the bedVisitDurableIid
     */
    public long getBedVisitDurableIid() {
        return bedVisitDurableIid;
    }

    /**
     * @param bedVisitDurableIid the bedVisitDurableIid to set
     */
    public void setBedVisitDurableIid(long bedVisitDurableIid) {
        this.bedVisitDurableIid = bedVisitDurableIid;
    }

    /**
     * @return the parentHospitalVisitDurableId
     */
    public long getParentHospitalVisitDurableId() {
        return parentHospitalVisitDurableId;
    }

    /**
     * @param parentHospitalVisitDurableId the parentHospitalVisitDurableId to set
     */
    public void setParentHospitalVisitDurableId(long parentHospitalVisitDurableId) {
        this.parentHospitalVisitDurableId = parentHospitalVisitDurableId;
    }

    /**
     * @return the parentBedVisitDurableId
     */
    public long getParentBedVisitDurableId() {
        return parentBedVisitDurableId;
    }

    /**
     * @param parentBedVisitDurableId the parentBedVisitDurableId to set
     */
    public void setParentBedVisitDurableId(long parentBedVisitDurableId) {
        this.parentBedVisitDurableId = parentBedVisitDurableId;
    }

    /**
     * @return the admissionTime
     */
    public Instant getAdmissionTime() {
        return admissionTime;
    }

    /**
     * @param admissionTime the admissionTime to set
     */
    public void setAdmissionTime(Instant admissionTime) {
        this.admissionTime = admissionTime;
    }

    /**
     * @return the dischargeTime
     */
    public Instant getDischargeTime() {
        return dischargeTime;
    }

    /**
     * @param dischargeTime the dischargeTime to set
     */
    public void setDischargeTime(Instant dischargeTime) {
        this.dischargeTime = dischargeTime;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

}
