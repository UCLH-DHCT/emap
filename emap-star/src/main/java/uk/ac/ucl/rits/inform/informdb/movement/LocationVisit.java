package uk.ac.ucl.rits.inform.informdb.movement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;

/**
 * This represents a patient being in a location for an amount of time. Every
 * location visit is part of a hospital visit, as you have to be in the hospital
 * before you can go to a specific location within it. Location visits can
 * optionally have a parent location visit. This happens when the patient is
 * still considered to be at the parent location (e.g. going down to an MRI
 * scanner from a ward bed doesn't vacate the ward bed).
 * @author UCL RITS
 */
@Entity
@JsonIgnoreProperties({})
@Table(indexes = {})
public class LocationVisit extends TemporalCore<LocationVisit> implements Serializable {

    private static final long serialVersionUID = -8228844390430073225L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long bedVisitId;

    @Column(nullable = false)
    private long parentHospitalVisitId;
    private long parentBedVisitId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant admissionTime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant dischargeTime;
    private String location;

    public LocationVisit() {}

    public LocationVisit(final LocationVisit other) {
        super(other);
        this.bedVisitId = other.bedVisitId;
        this.parentHospitalVisitId = other.parentHospitalVisitId;
        this.parentBedVisitId = other.parentBedVisitId;
        this.admissionTime = other.admissionTime;
        this.dischargeTime = other.dischargeTime;
        this.location = other.location;
    }

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
     * @return the parentHospitalVisitId
     */
    public long getParentHospitalVisitId() {
        return parentHospitalVisitId;
    }

    /**
     * @param parentHospitalVisitId the parentHospitalVisitId to set
     */
    public void setParentHospitalVisitId(long parentHospitalVisitId) {
        this.parentHospitalVisitId = parentHospitalVisitId;
    }

    /**
     * @return the parentBedVisitId
     */
    public long getParentBedVisitId() {
        return parentBedVisitId;
    }

    /**
     * @param parentBedVisitId the parentBedVisitId to set
     */
    public void setParentBedVisitId(long parentBedVisitId) {
        this.parentBedVisitId = parentBedVisitId;
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

    @Override
    public LocationVisit copy() {
        return new LocationVisit(this);
    }

}
