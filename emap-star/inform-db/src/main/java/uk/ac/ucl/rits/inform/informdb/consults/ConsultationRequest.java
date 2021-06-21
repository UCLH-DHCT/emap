package uk.ac.ucl.rits.inform.informdb.consults;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

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
    @JoinColumn(name = "consultRequestTypeId", nullable = false)
    private ConsultationType consultationTypeId;

    @ManyToOne
    @JoinColumn(name = "mnrId", nullable = false)
    private Mrn mrnId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /** Optional fields for consultation requests. */
    private Boolean closedDueToDischarge = false;
    private String comments;
    private String consultId;
    private Instant statusChangeTime;
    private Instant requestedDateTime;
    private Boolean cancelled = false;

    /**
     * Minimal information constructor.
     * @param consultationTypeId     ID for relevant consultation type
     * @param mrn                           ID of patient consultation request relates to
     * @param hospitalVisitId               ID for hospital visit of patient that consultation request relates to
     */
    public ConsultationRequest(ConsultationType consultationTypeId, Mrn mrn,
                               HospitalVisit hospitalVisitId) {
        this.consultationTypeId = consultationTypeId;
        this.mrnId = mrn;
        this.hospitalVisitId = hospitalVisitId;
    }

    /**
     * Build a new ConsultRequest from an existing one.
     * @param other existing ConsultRequest
     */
    public ConsultationRequest(ConsultationRequest other) {
        super(other);
        this.consultationTypeId = other.consultationTypeId;
        this.mrnId = other.mrnId;
        this.hospitalVisitId = other.hospitalVisitId;
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
