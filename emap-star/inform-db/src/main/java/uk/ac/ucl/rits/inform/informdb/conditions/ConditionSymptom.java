package uk.ac.ucl.rits.inform.informdb.conditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Id;
import java.time.Instant;

/**
 * \brief Symptoms of a condition that a patient can have so that it can be recognised by clinical staff.
 * <p>
 * Symptoms do not only occur in relation to diseases, but can also be used to characterise other conditions of a patient,
 * e.g. allergies.
 *
 * @author Anika Cawthorn
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class ConditionSymptom extends TemporalCore<ConditionSymptom, ConditionSymptomAudit> {
    /**
     * \brief Unique identifier in EMAP for this conditionSymptom record.
     * <p>
     * This is the primary key for the conditionSymptom table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long conditionSymptomId;

    /**
     * \brief Identifier for the ConditionType associated with this record.
     *
     * This is a foreign key that joins the conditionSymptom table to the patientCondition table.
     */
    @ManyToOne
    @JoinColumn(name = "patientConditionId", nullable = false)
    private PatientCondition patientConditionId;

    /**
     * \brief Human readable name for this conditionSymptom.
     */
    private String name;

    /**
     * \brief Mapping code for the observation from the standardised vocabulary system. Not yet implemented.
     */
    @Column(nullable = true)
    private String standardisedCode;

    /**
     * \brief Nomenclature or classification system used. Not yet implemented.
     */
    @Column(nullable = true)
    private String standardisedVocabulary;

    /**
     * Minimal information constructor.
     *
     * @param name       Name of the conditionSymptom, how it is referred to in the hospital.
     */
    public ConditionSymptom(String name, PatientCondition condition) {
        this.name = name;
        this.patientConditionId = condition;
    }

    /**
     * Build a new PatientStateType from an existing one.
     * @param other existing PatientStateType
     */
    public ConditionSymptom(ConditionSymptom other) {
        super(other);
        this.name = other.name;
        this.standardisedCode = other.standardisedCode;
        this.standardisedVocabulary = other.standardisedVocabulary;

    }

    @Override
    public ConditionSymptom copy() {
        return new ConditionSymptom(this);
    }

    @Override
    public ConditionSymptomAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConditionSymptomAudit(this, validUntil, storedUntil);
    }
}
