package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(name = "lv_hospital_visit_id", columnList = "hospitalVisitId"),
        @Index(name = "lv_location_id", columnList = "locationId")})
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AuditTable(indexes = {@Index(name = "lva_hospital_visit_id", columnList = "hospitalVisitId"),
        @Index(name = "lva_location_id", columnList = "locationId")})
public class LocationVisit extends TemporalCore<LocationVisit, LocationVisitAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long locationVisitId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    private Long parentLocationVisitId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant admissionTime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant dischargeTime;

    @OneToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;
    /**
     * Admission time has been inferred (not set from an A01, A02 or A03).
     */
    @Column(nullable = false)
    private Boolean inferredAdmission = false;
    /**
     * Discharge time has been inferred (not set from an A01, A02 or A03).
     */
    @Column(nullable = false)
    private Boolean inferredDischarge = false;

    public LocationVisit() {}

    /**
     * Create new location visit with all required information.
     * @param validFrom     Time of the message event
     * @param storedFrom    Time that emap-core encountered the message
     * @param location      Location
     * @param hospitalVisit Hospital visit
     */
    public LocationVisit(Instant validFrom, Instant storedFrom, Location location, HospitalVisit hospitalVisit) {
        setAdmissionTime(validFrom);
        setLocationId(location);
        setHospitalVisitId(hospitalVisit);
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    /**
     * Create Inferred location visit.
     * @param admissionTime inferred admission time
     * @param validFrom     time that the message was valid from
     * @param storedFrom    time that emap core stared processing the message
     * @param location      location for admission
     * @param hospitalVisit parent hospital visit
     */
    public LocationVisit(Instant admissionTime, Instant validFrom, Instant storedFrom, Location location, HospitalVisit hospitalVisit) {
        setAdmissionTime(admissionTime);
        setLocationId(location);
        setHospitalVisitId(hospitalVisit);
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    private LocationVisit(LocationVisit other) {
        super(other);
        this.locationVisitId = other.locationVisitId;
        this.hospitalVisitId = other.hospitalVisitId;
        this.parentLocationVisitId = other.parentLocationVisitId;
        this.admissionTime = other.admissionTime;
        this.dischargeTime = other.dischargeTime;
        this.locationId = other.locationId;
        this.inferredAdmission = other.inferredAdmission;
        this.inferredDischarge = other.inferredDischarge;
    }

    @Override
    public LocationVisit copy() {
        return new LocationVisit(this);
    }

    @Override
    public LocationVisitAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        return new LocationVisitAudit(this, validUntil, storedFrom);
    }

}
