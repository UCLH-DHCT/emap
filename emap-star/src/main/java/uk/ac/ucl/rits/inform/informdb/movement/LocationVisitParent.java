package uk.ac.ucl.rits.inform.informdb.movement;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Parent class that is not created as an entity to avoid polymorphic queries based on the original and audit table.
 * <p>
 * See {@link LocationVisit} for more details
 * @author UCL RITS
 */
@MappedSuperclass
public class LocationVisitParent extends TemporalCore<LocationVisitParent> implements Serializable {
    private static final long serialVersionUID = -8228844390430073225L;

    @Column(nullable = false)
    private long parentHospitalVisitId;
    private long parentBedVisitId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant admissionTime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant dischargeTime;

    @OneToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    public LocationVisitParent() {}

    public LocationVisitParent(final LocationVisitParent other) {
        super(other);
        this.parentHospitalVisitId = other.parentHospitalVisitId;
        this.parentBedVisitId = other.parentBedVisitId;
        this.admissionTime = other.admissionTime;
        this.dischargeTime = other.dischargeTime;
        this.locationId = other.locationId;
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
     * @return the locationId
     */
    public Location getLocation() {
        return locationId;
    }

    /**
     * @param locationId the location to set
     */
    public void setLocation(Location locationId) {
        this.locationId = locationId;
    }

    @Override
    public LocationVisitParent copy() {
        return new LocationVisitParent(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        LocationVisitParent that = (LocationVisitParent) o;
        return parentHospitalVisitId == that.parentHospitalVisitId
                && parentBedVisitId == that.parentBedVisitId
                && Objects.equals(admissionTime, that.admissionTime)
                && Objects.equals(dischargeTime, that.dischargeTime)
                && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentHospitalVisitId, parentBedVisitId, admissionTime, dischargeTime, locationId);
    }
}
