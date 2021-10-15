package uk.ac.ucl.rits.inform.informdb.consults;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * Holds information relevant to consultation requests for patients.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class ConsultationRequest extends TemporalCore<ConsultationRequest, ConsultationRequestAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long consultationRequestId;

    /**
     * Type of consult requested.
     */
    @ManyToOne
    @JoinColumn(name = "consultationTypeId", nullable = false)
    private ConsultationType consultationTypeId;

    /**
     * Foreign key to hospital visit.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /**
     * ID for the consult within EPIC.
     */
    @Column(nullable = false)
    private Long internalId;

    /**
     * Consult request was closed on discharge.
     */
    private Boolean closedDueToDischarge = false;
    /**
     * Notes added to the consult request which are not tied to a question.
     */
    @Column(columnDefinition = "text")
    private String comments;
    /**
     * Time that the consult has last been updated.
     */
    private Instant statusChangeTime;
    /**
     * Time that the consult was scheduled to occur at.
     */
    private Instant scheduledDatetime;
    /**
     * The consultation request has been cancelled by a user.
     */
    private Boolean cancelled = false;

    /**
     * Minimal information constructor.
     * @param consultationTypeId ID for relevant consultation type
     * @param hospitalVisitId    ID for hospital visit of patient that consultation request relates to
     * @param internalId         ID for consultation request
     */
    public ConsultationRequest(ConsultationType consultationTypeId, HospitalVisit hospitalVisitId, Long internalId) {
        this.consultationTypeId = consultationTypeId;
        this.hospitalVisitId = hospitalVisitId;
        this.internalId = internalId;
    }

    /**
     * Build a new ConsultationRequest from an existing one.
     * @param other existing ConsultationRequest
     */
    public ConsultationRequest(ConsultationRequest other) {
        super(other);
        this.consultationTypeId = other.consultationTypeId;
        this.hospitalVisitId = other.hospitalVisitId;
        this.internalId = other.internalId;
        this.closedDueToDischarge = other.closedDueToDischarge;
        this.comments = other.comments;
        this.statusChangeTime = other.statusChangeTime;
        this.scheduledDatetime = other.scheduledDatetime;
        this.cancelled = other.cancelled;
    }

    @Override
    public ConsultationRequest copy() {
        return new ConsultationRequest(this);
    }

    @Override
    public ConsultationRequestAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConsultationRequestAudit(this, validUntil, storedUntil);
    }
}
