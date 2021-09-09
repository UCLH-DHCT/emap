package uk.ac.ucl.rits.inform.informdb.decisions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * Holds information relevant to advanced decisions taken by patients.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class AdvancedDecision extends TemporalCore<AdvancedDecision, AdvancedDecisionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    /**
     * Identifier for specific advanced decision.
     */
    private long advancedDecisionId;

    @ManyToOne
    @JoinColumn(name = "advancedDecisionTypeId", nullable = false)
    /**
     * Identifier for type of specific advanced decision.
     */
    private AdvancedDecisionType advancedDecisionTypeId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    /**
     * Identifier for hospital visit of specific advanced decision.
     */
    private HospitalVisit hospitalVisitId;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    /**
     * Identifier for patient of specific advanced decision.
     */
    private Mrn mrnId;

    @Column(nullable = false)
    /**
     * Identifier assigned in source system that should be unique across all advanced decisions recorded in the
     * hospital.
     */
    private Long internalId;

    // Optional fields for advanced decisions.
    /**
     * Indicates whether (True) or not (False) an advanced decision ceased to exist based on patient discharge.
     */
    private Boolean closedDueToDischarge = false;

    /**
     * Time when the information for the advanced decision has been last updated.
     */
    private Instant statusChangeTime;

    /**
     * Time when the advanced decision for the patient's hospital visit has been recorded first.
     */
    private Instant requestedDatetime;

     /**
      * Indicates whether (True) or not (False) an advanced decision was cancelled, e.g. through a patient request.
      */
    private Boolean cancelled = false;

    /**
     * Minimal information constructor.
     * @param advancedDecisionTypeId    Identifier of AdvancedDecisionType relevant for this AdvancedDecision.
     * @param hospitalVisitId           Identifier of HospitalVisit this AdvancedDecision has been recorded for.
     * @param mrnId                     Patient identifier for whom AdvancedDecision is recorded.
     * @param internalId                Unique identifier assigned by EPIC for advanced decision.
     */
    public AdvancedDecision(AdvancedDecisionType advancedDecisionTypeId, HospitalVisit hospitalVisitId, Mrn mrnId,
                            Long internalId) {
        this.advancedDecisionTypeId = advancedDecisionTypeId;
        this.hospitalVisitId = hospitalVisitId;
        this.mrnId = mrnId;
        this.internalId = internalId;
    }

    /**
     * Build a new AdvancedDecision from an existing one.
     * @param other existing AdvancedDecision
     */
    public AdvancedDecision(AdvancedDecision other) {
        super(other);
        this.advancedDecisionId = other.advancedDecisionId;
        this.advancedDecisionTypeId = other.advancedDecisionTypeId;
        this.internalId = other.getInternalId();
        this.hospitalVisitId = other.hospitalVisitId;
        this.mrnId = other.mrnId;
        this.cancelled = other.cancelled;
        this.closedDueToDischarge = other.closedDueToDischarge;
        this.statusChangeTime = other.statusChangeTime;
        this.requestedDatetime = other.requestedDatetime;
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
