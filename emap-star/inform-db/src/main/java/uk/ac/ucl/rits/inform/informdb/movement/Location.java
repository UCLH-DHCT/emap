package uk.ac.ucl.rits.inform.informdb.movement;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * Known locations within the hospital.
 * @author UCL RITS
 */
@SuppressWarnings("serial")
@Entity
@Table
@Data
public class Location implements  Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long locationId;
    private String locationString;

    public Location() {
    }

    public Location(String locationString) {
        this.locationString = locationString;
    }

    @Override
    public String toString() {
        return String.format("Location{locationId=%d, locationString='%s'}", locationId, locationString);
    }
}
