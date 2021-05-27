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
public class ConsultRequestType extends TemporalCore<ConsultRequestType, ConsultRequestTypeAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long consultRequestTypeId;

    @Column(nullable = false)
    private String dataType;

    /**
     * disease or infection type.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Mininame constructor to create ConsultRequestType and add name and type.
     * @param name
     * @param dataType
     * @param validFrom
     * @param storedFrom
     */
    public ConsultRequestType(String name, String dataType, Instant validFrom, Instant storedFrom) {
        this.name = name;
        this.dataType = dataType;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    /**
     * Build a new ConsultReqyestType from an existing one.
     * @param other existing ConsultRequestType
     */
    public ConsultRequestType(ConsultRequestType other) {
        super(other);
        this.dataType = other.dataType;
        this.name = other.name;
    }

    @Override
    public ConsultRequestType copy() {
        return new ConsultRequestType(this);
    }

    @Override
    public ConsultRequestTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConsultRequestTypeAudit(this, validUntil, storedUntil);
    }
}
