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
 * Represents patient conditions that start and can end.
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
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long patientConditionId;

    @ManyToOne
    @JoinColumn(name = "conditionTypeId", nullable = false)
    private ConditionType conditionTypeId;

    private Long internalId;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId")
    private HospitalVisit hospitalVisitId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant addedDateTime;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant resolutionDateTime;

    private LocalDate onsetDate;

    /**
     * temporary infection?
     */
    private String classification;

    private String status;

    private String priority;

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

