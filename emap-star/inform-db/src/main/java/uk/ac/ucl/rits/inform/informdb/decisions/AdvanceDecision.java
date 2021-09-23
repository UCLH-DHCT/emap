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
 * Holds information relevant to advance decisions taken by patients.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class AdvanceDecision extends TemporalCore<AdvanceDecision, AdvanceDecisionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    /**
     * Identifier for specific advance decision.
     */
    private long advanceDecisionId;

    @ManyToOne
    @JoinColumn(name = "advanceDecisionTypeId", nullable = false)
    /**
     * Identifier for type of specific advance decision.
     */
    private AdvanceDecisionType advanceDecisionTypeId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    /**
     * Identifier for hospital visit of specific advance decision.
     */
    private HospitalVisit hospitalVisitId;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    /**
     * Identifier for patient of specific advance decision.
     */
    private Mrn mrnId;

    @Column(nullable = false)
    /**
     * Identifier assigned in source system that should be unique across all advance decisions recorded in the
     * hospital.
     */
    private Long internalId;

    // Optional fields for advance decisions.
    /**
     * Indicates whether (True) or not (False) an advance decision ceased to exist based on patient discharge.
     */
    private Boolean closedDueToDischarge = false;

    /**
     * Time when the information for the advance decision has been last updated.
     */
    private Instant statusChangeTime;

    /**
     * Time when the advance decision for the patient's hospital visit has been recorded first.
     */
    private Instant requestedDatetime;

     /**
      * Indicates whether (True) or not (False) an advance decision was cancelled, e.g. through a patient request.
      */
    private Boolean cancelled = false;

    /**
     * Minimal information constructor.
     * @param advanceDecisionTypeId    Identifier of AdvanceDecisionType relevant for this AdvanceDecision.
     * @param hospitalVisitId           Identifier of HospitalVisit this AdvanceDecision has been recorded for.
     * @param mrnId                     Patient identifier for whom AdvanceDecision is recorded.
     * @param internalId                Unique identifier assigned by EPIC for advance decision.
     */
    public AdvanceDecision(AdvanceDecisionType advanceDecisionTypeId, HospitalVisit hospitalVisitId, Mrn mrnId,
                           Long internalId) {
        this.advanceDecisionTypeId = advanceDecisionTypeId;
        this.hospitalVisitId = hospitalVisitId;
        this.mrnId = mrnId;
        this.internalId = internalId;
    }

    /**
     * Build a new AdvanceDecision from an existing one.
     * @param other existing AdvanceDecision
     */
    public AdvanceDecision(AdvanceDecision other) {
        super(other);
        this.advanceDecisionId = other.advanceDecisionId;
        this.advanceDecisionTypeId = other.advanceDecisionTypeId;
        this.internalId = other.getInternalId();
        this.hospitalVisitId = other.hospitalVisitId;
        this.mrnId = other.mrnId;
        this.cancelled = other.cancelled;
        this.closedDueToDischarge = other.closedDueToDischarge;
        this.statusChangeTime = other.statusChangeTime;
        this.requestedDatetime = other.requestedDatetime;
    }

    @Override
    public AdvanceDecision copy() {
        return new AdvanceDecision(this);
    }

    @Override
    public AdvanceDecisionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new AdvanceDecisionAudit(this, validUntil, storedUntil);
    }
}
