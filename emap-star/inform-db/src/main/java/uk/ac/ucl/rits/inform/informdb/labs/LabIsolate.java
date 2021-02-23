package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * Isolates identified from culture.
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabIsolate extends TemporalCore<LabIsolate, LabIsolateAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labIsolateId;

    @ManyToOne
    @JoinColumn(name = "labResultId", nullable = false)
    private LabResult labResultId;

    /**
     * Lab system's code for the isolate.
     */
    private String isolateCode;
    /**
     * Name of the isolate.
     */
    private String isolateName;
    /**
     * Method of culture.
     */
    private String cultureType;
    /**
     * Usually CFU range, but can also be categorical.
     */
    private String quantity;


    public LabIsolate() {}

    public LabIsolate(LabResult labResultId, String isolateCode) {
        this.labResultId = labResultId;
        this.isolateCode = isolateCode;
    }


    public LabIsolate(LabIsolate other) {
        super(other);
        this.labIsolateId = other.labIsolateId;
        this.labResultId = other.labResultId;
        this.isolateCode = other.isolateCode;
        this.isolateName = other.isolateName;
        this.cultureType = other.cultureType;
        this.quantity = other.quantity;
    }


    @Override
    public LabIsolate copy() {
        return new LabIsolate(this);
    }

    @Override
    public LabIsolateAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabIsolateAudit(this, validUntil, storedUntil);
    }
}
