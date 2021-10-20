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
import javax.persistence.Id;
import java.time.Instant;

/**
 * \brief Type of condition that a patient can have.
 *
 * Types are defined by the dataType (problem list or infection) and the name (problem list-> diagnosis; infection -> infection name)
 * @author Anika Cawthorn
 * @author Stef Piatek
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class ConditionType extends TemporalCore<ConditionType, ConditionTypeAudit> {

    /**
     * \brief Unique identifier in EMAP for this ConditionType record.
     *
     * This is the primary key for the ConditionType table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long conditionTypeId;

    /**
     * \brief Problem list or patient infection.
     */
    @Column(nullable = false)
    private String dataType;

    /**
     * \brief Code used within EPIC for this ConditionType.
     */
    @Column(nullable = false)
    private String internalCode;

    /**
     * \brief Human readable name for this ConditionType.
     */
    private String name;

    /**
     * \brief Not yet implemented.
     */
    private String standardisedCode;

    /**
     * \brief Not yet implemented.
     */
    private String standardisedVocabulary;

    /**
     * Minimal information constructor.
     * @param dataType   Type of patient state type; either patient infection or problem list
     * @param code       EPIC code of the patient state type
     * @param validFrom  Timestamp from which information valid from
     * @param storedFrom Timestamp from which information stored from
     */
    public ConditionType(String dataType, String code, Instant validFrom, Instant storedFrom) {
        this.internalCode = code;
        this.dataType = dataType;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    /**
     * Build a new PatientStateType from an existing one.
     * @param other existing PatientStateType
     */
    public ConditionType(ConditionType other) {
        super(other);
        this.dataType = other.dataType;
        this.internalCode = other.internalCode;
        this.name = other.name;
        this.standardisedCode = other.standardisedCode;
        this.standardisedVocabulary = other.standardisedVocabulary;

    }

    @Override
    public ConditionType copy() {
        return new ConditionType(this);
    }

    @Override
    public ConditionTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConditionTypeAudit(this, validUntil, storedUntil);
    }

}
