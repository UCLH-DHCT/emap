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
 * \brief Holds information relevant to advance decisions taken by patients.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class AdvanceDecision extends TemporalCore<AdvanceDecision, AdvanceDecisionAudit> {

    /**
     * \brief Unique identifier in EMAP for this AdvancedDecision record.
     *
     * This is the primary key for the AdvanceDecision table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long advanceDecisionId;

    /**
     * \brief Identifier for the AdvanceDecisionType associated with this record.
     *
     * This is a foreign key that joins the AdvanceDecisionType table to the AdvanceDecision table.
     */
    @ManyToOne
    @JoinColumn(name = "advanceDecisionTypeId", nullable = false)
    private AdvanceDecisionType advanceDecisionTypeId;

    /**
     * \brief Identifier for the HospitalVisit associated with this record.
     *
     * This is a foreign key that joins the AdvanceDecision table to the HospitalVisit table.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /**
     * \brief Identifier for the Mrn associated with this record.
     *
     * This is a foreign key that joins the AdvanceDecision table to the Mrn table.
     */
    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    /**
     * \brief Unique identifier assigned in source system.
     *
     * This identifier should be unique across all advance decisions recorded in the hospital.
     */
    @Column(nullable = false, unique = true)
    private Long internalId;

    // Optional fields for advance decisions.
    /**
     * \brief Indicates whether (True) or not (False) an advance decision ceased to exist based on patient discharge.
     */
    private Boolean closedDueToDischarge = false;

    /**
     * \brief Time when the information for the advance decision has been last updated.
     */
    private Instant statusChangeDatetime;

    /**
     * \brief Time when the advance decision for the patient's hospital visit has been recorded first.
     */
    private Instant requestedDatetime;

     /**
      * \brief Indicates whether (True) or not (False) an advance decision was cancelled, e.g. through a patient request.
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
        this.statusChangeDatetime = other.statusChangeDatetime;
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
