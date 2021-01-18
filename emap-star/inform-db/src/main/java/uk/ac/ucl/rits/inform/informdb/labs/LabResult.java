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
 * A LabResult is a single component result of a lab. A single order or sample
 * is likely to produce several results.
 * @author Roma Klapaukh
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabResult extends TemporalCore<LabResult, LabResultAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labResultId;

    @ManyToOne
    @JoinColumn(name = "labNumberId", nullable = false)
    private LabNumber labNumberId;

    @ManyToOne
    @JoinColumn(name = "labTestDefinitionId", nullable = false)
    private LabTestDefinition labTestDefinitionId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant resultLastModifiedTime;

    private Boolean abnormal;
    private String resultAsText;
    private Double resultAsReal;

    /**
     * For numeric results, defines the operator used to define the value.
     *
     * For example an estimated GFR is given as `>90`, this would have a valueAsReal of `90` and a numeric operator of `>`
     * Most of the time, values are precise so have a `=` result operator
     */
    private String resultOperator;
    private Double rangeHigh;
    private Double rangeLow;

    private String comment;

    public LabResult() {}

    public LabResult(LabResult other) {
        super(other);

        this.labResultId = other.labResultId;
        this.labNumberId = other.labNumberId;
        this.labTestDefinitionId = other.labTestDefinitionId;
        this.resultLastModifiedTime = other.resultLastModifiedTime;
        this.abnormal = other.abnormal;
        this.resultAsText = other.resultAsText;
        this.resultAsReal = other.resultAsReal;
        this.resultOperator = other.resultOperator;
        this.rangeHigh = other.rangeHigh;
        this.rangeLow = other.rangeLow;
        this.comment = other.comment;
    }

    public LabResult(LabNumber labNumberId, LabTestDefinition labTestDefinitionId, Instant resultLastModifiedTime)  {
        this.labNumberId = labNumberId;
        this.labTestDefinitionId = labTestDefinitionId;
        this.resultLastModifiedTime = resultLastModifiedTime;
    }

    @Override
    public LabResult copy() {
        return new LabResult(this);
    }

    @Override
    public LabResultAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabResultAudit(this, validUntil, storedUntil);
    }
}
