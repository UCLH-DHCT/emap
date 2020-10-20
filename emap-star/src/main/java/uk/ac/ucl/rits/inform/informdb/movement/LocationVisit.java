package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class LocationVisit extends LocationVisitParent {
    private static final long serialVersionUID = 2671789121005769008L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long locationVisitId;

    public LocationVisit() {
    }

    /**
     * Create new location visit with all required information.
     * @param validFrom    Time of the message event
     * @param storedFrom   Time that emap-core encountered the message
     * @param location     Location
     * @param sourceSystem source system
     */
    public LocationVisit(Instant validFrom, Instant storedFrom, Location location, HospitalVisit hospitalVisit, String sourceSystem) {
        super();
        setAdmissionTime(validFrom);
        setLocation(location);
        setSourceSystem(sourceSystem);
        setHospitalVisitId(hospitalVisit);
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    public LocationVisit(LocationVisit other) {
        super(other);
        locationVisitId = other.locationVisitId;
    }

    @Override
    public LocationVisit copy() {
        return new LocationVisit(this);
    }

}
