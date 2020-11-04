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
public abstract class LocationVisitParent extends TemporalCore<LocationVisit, LocationVisitAudit> implements Serializable {
    private static final long serialVersionUID = -8228844390430073225L;


    private Long parentLocationVisitId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant admissionTime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant dischargeTime;

    /**
     * The source system from which we learnt about this hospital visit.
     */
    @Column(nullable = false)
    private String sourceSystem;

    @OneToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    public LocationVisitParent() {}

    public LocationVisitParent(final LocationVisitParent other) {
        super(other);
        this.parentLocationVisitId = other.parentLocationVisitId;
        this.sourceSystem = other.sourceSystem;
        this.admissionTime = other.admissionTime;
        this.dischargeTime = other.dischargeTime;
        this.locationId = other.locationId;
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
     * @return source system.
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem to set.
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        LocationVisitParent that = (LocationVisitParent) o;
        return parentLocationVisitId == that.parentLocationVisitId
                && sourceSystem == that.sourceSystem
                && Objects.equals(admissionTime, that.admissionTime)
                && Objects.equals(dischargeTime, that.dischargeTime)
                && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentLocationVisitId, sourceSystem, admissionTime, dischargeTime, locationId);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("LocationVisitParent{")
                .append("parentLocationVisitId=")
                .append(parentLocationVisitId)
                .append(", admissionTime=")
                .append(admissionTime)
                .append(", dischargeTime=")
                .append(dischargeTime)
                .append(", sourceSystem='")
                .append(sourceSystem)
                .append("', locationId=")
                .append(locationId)
                .append('}').toString();
    }
}
