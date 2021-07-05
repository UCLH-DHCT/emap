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

    @ManyToOne
    @JoinColumn(name = "consultationTypeId", nullable = false)
    private ConsultationType consultationTypeId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    @Column(nullable = false)
    private Long consultId;

    /** Optional fields for consultation requests. */
    private Boolean closedDueToDischarge = false;
    private String comments;
    private Instant statusChangeTime;
    private Instant requestedDateTime;
    private Boolean cancelled = false;

    /**
     * Minimal information constructor.
     * @param consultationTypeId    ID for relevant consultation type
     * @param hospitalVisitId       ID for hospital visit of patient that consultation request relates to
     * @param consultId             ID for consultation request
     */
    public ConsultationRequest(ConsultationType consultationTypeId, HospitalVisit hospitalVisitId, Long consultId) {
        this.consultationTypeId = consultationTypeId;
        this.hospitalVisitId = hospitalVisitId;
        this.consultId = consultId;
    }

    /**
     * Build a new ConsultationRequest from an existing one.
     * @param other existing ConsultationRequest
     */
    public ConsultationRequest(ConsultationRequest other) {
        super(other);
        this.consultationTypeId = other.consultationTypeId;
        this.hospitalVisitId = other.hospitalVisitId;
        this.consultId = other.consultId;
        this.closedDueToDischarge = other.closedDueToDischarge;
        this.comments = other.comments;
        this.statusChangeTime = other.statusChangeTime;
        this.requestedDateTime = other.requestedDateTime;
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
