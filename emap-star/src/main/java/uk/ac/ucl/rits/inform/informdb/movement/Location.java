package uk.ac.ucl.rits.inform.informdb.movement;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Known locations within the hospital.
 * @author UCL RITS
 */
@Entity
@Table
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long locationId;
    private String locationString;

    public Location() {
    }

    public Location(String locationString) {
        this.locationString = locationString;
    }

    /**
     * @return location Id.
     */
    public long getLocationId() {
        return locationId;
    }

    /**
     * @param locationId to set.
     */
    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    /**
     * @return Location String.
     */
    public String getLocationString() {
        return locationString;
    }

    /**
     * @param locationString to set.
     */
    public void setLocationString(String locationString) {
        this.locationString = locationString;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Location location = (Location) o;
        return locationId == location.locationId
                && Objects.equals(locationString, location.locationString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, locationString);
    }

    @Override
    public String toString() {
        return String.format("Location{locationId=%d, locationString='%s'}", locationId, locationString);
    }
}
