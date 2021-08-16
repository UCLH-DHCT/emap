package uk.ac.ucl.rits.inform.informdb.decisions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

/**
 * Holds information relevant to advanced decisions taken by patients.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AuditTable
public class AdvancedDecision extends TemporalCore<AdvancedDecision, AdvancedDecisionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long advancedDecisionId;

    /**
     * Minimal information constructor.
     */
    public AdvancedDecision() {

    }

    /**
     * Build a new AdvancedDecision from an existing one.
     * @param other existing AdvancedDecision
     */
    public AdvancedDecision(AdvancedDecision other) {
        super(other);

    }

    @Override
    public AdvancedDecision copy() {
        return new AdvancedDecision(this);
    }

    @Override
    public AdvancedDecisionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new AdvancedDecisionAudit(this, validUntil, storedUntil);
    }
}
