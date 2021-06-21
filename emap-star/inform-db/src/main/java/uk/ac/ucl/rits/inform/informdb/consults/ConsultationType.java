package uk.ac.ucl.rits.inform.informdb.consults;

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
 * Type of a consultation request made for a patient.
 * @author Anika Cawthorn
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AuditTable
public class ConsultationType extends TemporalCore<ConsultationType, ConsultationRequestTypeAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long consultationTypeId;
    @Column(nullable = false)
    private String standardisedCode;
    private String name;
    /**
     * Minimal constructor to create ConsultRequestType and add name for it. As there's only one field for now, it's
     * also the only constructor required for now.
     * @param standardisedCode  Consultation type as provided in ConsultationRequest message
     * @param validFrom         From which point in time the ConsultationType is valid
     * @param storedFrom        Time point at which ConsultationType was stored first
     */
    public ConsultationType(String standardisedCode, Instant validFrom, Instant storedFrom) {
        this.standardisedCode = standardisedCode;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }
    /**
     * Build a new ConsultationType from an existing one.
     * @param other existing ConsultationType
     */
    public ConsultationType(ConsultationType other) {
        super(other);
        this.standardisedCode = other.standardisedCode;
        this.name = other.name;
    }

    @Override
    public ConsultationType copy() {
        return new ConsultationType(this);
    }

    @Override
    public ConsultationRequestTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConsultationRequestTypeAudit(this, validUntil, storedUntil);
    }
}
