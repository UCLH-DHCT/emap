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
import java.util.HashMap;

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
    private ConsultationRequestType consultationRequestTypeId;

    @ManyToOne
    @JoinColumn(name = "mnrId", nullable = false)
    private Mrn mrnId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /** optional fields */
//    private boolean cancelledDueToDischarge;
//    private String notes;
//    private Instant statusChangeTime;
//    private Instant requestedDateTime;
//    private HashMap<String, String> questions;
//    private boolean cancelled;


    /**
     * Minimal information constructor.
     */
    public ConsultationRequest(ConsultationRequestType consultationRequestTypeId, Mrn mrn,
                               HospitalVisit hospitalVisitId) {
        this.consultationRequestTypeId = consultationRequestTypeId;
        this.mrnId = mrn;
        this.hospitalVisitId = hospitalVisitId;
    }

    /**
     * Build a new ConsultRequest from an existing one.
     * @param other existing ConsultRequest
     */
    public ConsultationRequest(ConsultationRequest other) {
        super(other);
        this.consultationRequestTypeId = other.consultationRequestTypeId;
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
