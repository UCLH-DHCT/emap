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

/**
 * Advanced decisions can be different in nature and therefore are distinguished by their type. Each of these types is
 * identified by a care code and name, e.g. "DNACPR" with care code "COD4".
 * @author Anika Cawthorn
 */
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
     * Name of the advanced decision type, e.g. DNACPR
     */
    @Column(nullable = false)
    private String name;

    /**
     * Code used within EPIC for advanced decision type, e.g COD4 for DNACPR.
     */
    @Column(nullable = false, unique = true)
    private String careCode;

    /**
     * Minimal information constructor.
     * @param name          Name of advanced decision type.
     * @param careCode      Care code relating to advanced decision type.
     * @param validFrom     Timestamp from which information valid from
     * @param storedFrom    Timestamp from which information stored from
     */
    public AdvancedDecisionType(String careCode, String name, Instant validFrom, Instant storedFrom) {
        this.name = name;
        this.careCode = careCode;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    /**
     * Build a new AdvancedDecisionType from an existing one.
     * @param other existing AdvancedDecisionType
     */
    public AdvancedDecisionType(AdvancedDecisionType other) {
        super(other);
        this.advancedDecisionTypeId = other.getAdvancedDecisionTypeId();
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
