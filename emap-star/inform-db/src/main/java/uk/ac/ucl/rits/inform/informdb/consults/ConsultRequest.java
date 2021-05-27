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
public class ConsultRequest extends TemporalCore<ConsultRequest, ConsultRequestAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long consultRequestId;

    @ManyToOne
    @JoinColumn(name = "consultRequestTypeId", nullable = false)
    private ConsultRequestType consultRequestTypeId;

    @ManyToOne
    @JoinColumn(name = "mnrId", nullable = false)
    private Mrn mrnId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId")
    private HospitalVisit hospitalVisitId;
    /**
     * Build a new ConsultRequest from an existing one.
     * @param other existing ConsultRequest
     */
    public ConsultRequest(ConsultRequest other) {
        super(other);
        this.consultRequestTypeId = other.consultRequestTypeId;
        this.mrnId = other.mrnId;
        if (other.hospitalVisitId != null) {
            this.hospitalVisitId = other.hospitalVisitId;
        }
//        this.addedDateTime = other.addedDateTime;
//        this.resolutionDateTime = other.resolutionDateTime;
//        this.onsetDate = other.onsetDate;
//        this.classification = other.classification;
//        this.status = other.status;
//        this.priority = other.priority;
//        this.comment = other.comment;
    }

    @Override
    public ConsultRequest copy() {
        return new ConsultRequest(this);
    }

    @Override
    public ConsultRequestAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConsultRequestAudit(this, validUntil, storedUntil);
    }
}
