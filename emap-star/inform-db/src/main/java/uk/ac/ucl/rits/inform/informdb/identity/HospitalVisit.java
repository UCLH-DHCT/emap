package uk.ac.ucl.rits.inform.informdb.identity;

import java.time.Instant;

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
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

/**
 * This a single visit to the hospital. This is not necessarily an inpatient
 * visit, but includes outpatients, imaging, etc.
 *
 * @author UCL RITS
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(indexes = { @Index(name = "encounterIndex", columnList = "encounter", unique = false) })
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AuditTable
public class HospitalVisit extends TemporalCore<HospitalVisit, HospitalVisitAudit> {


    /**
     * A primary key for this row.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long              hospitalVisitId;

    /**
     * The MRN this hospital visit happened under.
     */
    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    /**
     * The source system from which we learnt about this hospital visit.
     */
    @Column(nullable = false)
    private String sourceSystem;

    /**
     * The time the patient was first seen in the hospital as part of their visit.
     * This may be prior to their admission.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant presentationTime;

    /**
     * The time the patient was formally admitted.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant admissionTime;

    /**
     * The time the patient was discharged.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant dischargeTime;

    /**
     * The patient class. E.g. Inpatient or Outpaitent.
     */
    private String patientClass;

    /**
     * The patient's arrival method at hospital.
     */
    private String arrivalMethod;

    /**
     * Where the patient went after their departure.
     */
    private String dischargeDestination;

    /**
     * The patient's disposition on departure.
     */
    private String dischargeDisposition;

    /**
     * The source system identifier of this hospital visit. In Epic this corresponds
     * to the CSN.
     */
    @Column(nullable = false, unique = true)
    protected String encounter;

    /**
     * Default constructor.
     */
    public HospitalVisit() {}

    /**
     * Build a hospital visit from an existing object.
     *
     * @param other hospital visit
     */
    private HospitalVisit(HospitalVisit other) {
        super(other);
        hospitalVisitId = other.hospitalVisitId;
        this.encounter = other.encounter;
        this.admissionTime = other.admissionTime;
        this.arrivalMethod = other.arrivalMethod;
        this.dischargeDestination = other.dischargeDestination;
        this.dischargeDisposition = other.dischargeDisposition;
        this.dischargeTime = other.dischargeTime;
        this.mrnId = other.mrnId;
        this.patientClass = other.patientClass;
        this.presentationTime = other.presentationTime;
        this.sourceSystem = other.sourceSystem;
        this.setEncounter(other.getEncounter());
    }

    @Override
    public HospitalVisit copy() {
        return new HospitalVisit(this);
    }

    @Override
    public HospitalVisitAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        return new HospitalVisitAudit(this, validUntil, storedFrom);
    }
}
