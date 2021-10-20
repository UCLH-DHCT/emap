package uk.ac.ucl.rits.inform.informdb.conditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;
import java.time.LocalDate;

/**
 * \brief Represents patient conditions that start and can end.
 *
 * Currently envisaged as storing infection control's patient infection information and problem lists.
 * @author Anika Cawthorn
 * @author Stef Piatek
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AuditTable
public class PatientCondition extends TemporalCore<PatientCondition, PatientConditionAudit> {

    /**
     * \brief Unique identifier in EMAP for this PatientCondition record.
     *
     * This is the primary key for the PatientCondition table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long patientConditionId;

    /**
     * \brief Identifier for the ConditionType associated with this record.
     *
     * This is a foreign key that joins the PatientCondition table to the ConditionType table.
     */
    @ManyToOne
    @JoinColumn(name = "conditionTypeId", nullable = false)
    private ConditionType conditionTypeId;

    /**
     * \brief Identifier used in EPIC for this PatientCondition.
     */
    private Long internalId;

    /**
     * \brief Identifier for the Mrn associated with this record.
     *
     * This is a foreign key that joins the PatientCondition table to the Mrn table.
     */
    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    /**
     * \brief Identifier for the HospitalVisit associated with this record.
     *
     * This is a foreign key that joins the PatientCondition table to the HospitalVisit table.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId")
    private HospitalVisit hospitalVisitId;

    /**
     * \brief Date this record was added to database.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant addedDateTime;

    /**
     * \brief Date the PatientCondition referred to in this record was resolved.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant resolutionDateTime;

    /**
     * \brief Date at which the PatientCondition was first observed in the patient.
     */
    private LocalDate onsetDate;

    /**
     * \brief ?
     */
    private String classification;

    /**
     * \brief ?
     */
    private String status;

    /**
     * \brief ?
     */
    private String priority;

    /**
     * Comments added by clincian
     */
    @Column(columnDefinition = "text")
    private String comment;

    /**
     * Minimal information constructor.
     * @param internalId      Id in epic for the patient condition
     * @param conditionTypeId ID for patient state type
     * @param mrn             patient ID
     * @param addedDateTime   when patient state has been added
     */
    public PatientCondition(Long internalId, ConditionType conditionTypeId, Mrn mrn, Instant addedDateTime) {
        this.conditionTypeId = conditionTypeId;
        this.mrnId = mrn;
        this.addedDateTime = addedDateTime;
        this.internalId = internalId;
    }

    /**
     * Build a new PatientState from an existing one.
     * @param other existing PatientState
     */
    public PatientCondition(PatientCondition other) {
        super(other);
        patientConditionId = other.patientConditionId;
        conditionTypeId = other.conditionTypeId;
        mrnId = other.mrnId;
        if (other.hospitalVisitId != null) {
            hospitalVisitId = other.hospitalVisitId;
        }
        internalId = other.internalId;
        addedDateTime = other.addedDateTime;
        resolutionDateTime = other.resolutionDateTime;
        onsetDate = other.onsetDate;
        classification = other.classification;
        status = other.status;
        priority = other.priority;
        comment = other.comment;
    }

    @Override
    public PatientCondition copy() {
        return new PatientCondition(this);
    }

    @Override
    public PatientConditionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new PatientConditionAudit(this, validUntil, storedUntil);
    }
}

