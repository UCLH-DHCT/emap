package uk.ac.ucl.rits.inform.informdb.movement;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
}
