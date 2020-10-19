package uk.ac.ucl.rits.inform.informdb.movement;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import java.io.Serializable;
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

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;
    private long parentLocationVisitId;

    @OneToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    public LocationVisitParent() {}

    public LocationVisitParent(final LocationVisitParent other) {
        super(other);
        this.hospitalVisitId = other.hospitalVisitId;
        this.parentLocationVisitId = other.parentLocationVisitId;
        this.locationId = other.locationId;
    }


    /**
     * @return the parentHospitalVisitId
     */
    public HospitalVisit getHospitalVisitId() {
        return hospitalVisitId;
    }

    /**
     * @param hospitalVisitId the parentHospitalVisitId to set
     */
    public void setHospitalVisitId(HospitalVisit hospitalVisitId) {
        this.hospitalVisitId = hospitalVisitId;
    }

    /**
     * @return the parentLocationVisitId
     */
    public long getParentLocationVisitId() {
        return parentLocationVisitId;
    }

    /**
     * @param parentLocationVisitId the parentLocationVisitId to set
     */
    public void setParentLocationVisitId(long parentLocationVisitId) {
        this.parentLocationVisitId = parentLocationVisitId;
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
        return hospitalVisitId == that.hospitalVisitId
                && parentLocationVisitId == that.parentLocationVisitId
                && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hospitalVisitId, parentLocationVisitId, locationId);
    }
}
