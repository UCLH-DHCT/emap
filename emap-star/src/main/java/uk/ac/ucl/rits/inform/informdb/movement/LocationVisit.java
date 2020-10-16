package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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

    public LocationVisit(LocationVisit other) {
        super(other);
        locationVisitId = other.locationVisitId;
    }

    @Override
    public LocationVisit copy() {
        return new LocationVisit(this);
    }

}
