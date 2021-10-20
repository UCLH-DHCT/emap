package uk.ac.ucl.rits.inform.informdb.movement;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

    /**
     * \brief Unique identifier in EMAP for this location record.
     *
     * This is the primary key for the Location table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long locationId;
    @Column(nullable = false)
    private String locationString;

    @ManyToOne
    @JoinColumn(name = "departmentId")
    private Department departmentId;

    @ManyToOne
    @JoinColumn(name = "roomId")
    private Room roomId;

    @ManyToOne
    @JoinColumn(name = "bedId")
    private Bed bedId;

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
