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
public class ConsultationRequestType extends TemporalCore<ConsultationRequestType, ConsultationRequestTypeAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long consultationRequestTypeId;
    @Column(nullable = false)
    private String standardisedCode;
    private String name;
    /**
     * Minimal constructor to create ConsultRequestType and add name for it. As there's only one field for now, it's
     * also the only constructor required for now.
     * @param standardisedCode  Consultancy type as provided in ConsultRequest message
     * @param validFrom         From which point in time the ConsultRequestType is valid
     * @param storedFrom        Time point at which ConsultRequestType was stored first
     */
    public ConsultationRequestType(String standardisedCode, Instant validFrom, Instant storedFrom) {
        this.standardisedCode = standardisedCode;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }
    /**
     * Build a new ConsultRequestType from an existing one.
     * @param other existing ConsultRequestType
     */
    public ConsultationRequestType(ConsultationRequestType other) {
        super(other);
        this.standardisedCode = other.standardisedCode;
        this.name = other.name;
    }

    @Override
    public ConsultationRequestType copy() {
        return new ConsultationRequestType(this);
    }

    @Override
    public ConsultationRequestTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConsultationRequestTypeAudit(this, validUntil, storedUntil);
    }
}
