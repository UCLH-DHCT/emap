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

    @OneToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    public LocationVisitParent() {}

    public LocationVisitParent(final LocationVisitParent other) {
        super(other);
        this.parentHospitalVisitId = other.parentHospitalVisitId;
        this.parentBedVisitId = other.parentBedVisitId;
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
                && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentHospitalVisitId, parentBedVisitId, locationId);
    }
}
