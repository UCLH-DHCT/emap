package uk.ac.ucl.rits.inform.informdb.decisions;

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

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class AdvancedDecisionType extends TemporalCore<AdvancedDecisionType, AdvancedDecisionTypeAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long advancedDecisionTypeId;

    /**
     * problem list or patient infection.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Code used within EPIC for condition.
     */
    @Column(nullable = false)
    private String careCode;

    /**
     * Minimal information constructor.
     * @param name          Name of advanced decision type.
     * @param careCode      Care code relating to advanced decision type.
     * @param validFrom     Timestamp from which information valid from
     * @param storedFrom    Timestamp from which information stored from
     */
    public AdvancedDecisionType(String name, String careCode, Instant validFrom, Instant storedFrom) {
        this.name = name;
        this.careCode = careCode;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    /**
     * Build a new AdvancedDecisionType from an existing one.
     * @param other existing PatientStateType
     */
    public AdvancedDecisionType(AdvancedDecisionType other) {
        super(other);
        this.name = other.name;
        this.careCode = other.careCode;
    }

    @Override
    public AdvancedDecisionType copy() {
        return new AdvancedDecisionType(this);
    }

    @Override
    public AdvancedDecisionTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new AdvancedDecisionTypeAudit(this, validUntil, storedUntil);
    }
}
