package uk.ac.ucl.rits.inform.informdb.state;

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
 * Type of state that a patient can have.
 * Types are defined by the dataType (problem list or infection) and the name (problem list-> diagnosis; infection -> infection name)
 * @author Anika Cawthorn
 * @author Stef Piatek
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class PatientStateType extends TemporalCore<PatientStateType, PatientStateTypeAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long patientStateTypeId;

    /**
     * problem list or patient infection.
     */
    @Column(nullable = false)
    private String dataType;

    /**
     * disease or infection type.
     */
    @Column(nullable = false)
    private String name;
    private String standardisedCode;
    private String standardisedVocabulary;


    /**
     * Build a new PatientStateType from an existing one.
     * @param other existing PatientStateType
     */
    public PatientStateType(PatientStateType other) {
        super(other);
        this.dataType = other.dataType;
        this.name = other.name;
        this.standardisedCode = other.standardisedCode;
        this.standardisedVocabulary = other.standardisedVocabulary;

    }

    @Override
    public PatientStateType copy() {
        return new PatientStateType(this);
    }

    @Override
    public PatientStateTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new PatientStateTypeAudit(this, validUntil, storedUntil);
    }

}
