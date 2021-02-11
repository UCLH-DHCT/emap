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
 * Sensitivities show the affect of specific agents on isolates from cultures.
 * @author Roma Klapaukh
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabResultSensitivity extends TemporalCore<LabResultSensitivity, LabResultSensitivityAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labResultSensitivityId;

    @ManyToOne
    @JoinColumn(name = "labResultId", nullable = false)
    private LabResult labResultId;

    /**
     * The chemical (often antibiotic) used.
     */
    private String agent;
    /**
     * Sensitivity of the microbe to the agent.
     */
    private String sensitivity;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant reportingDatetime;

    public LabResultSensitivity() {}

    public LabResultSensitivity(LabResult labResultId, String agent) {
        this.labResultId = labResultId;
        this.agent = agent;
    }


    public LabResultSensitivity(LabResultSensitivity other) {
        super(other);
        this.labResultSensitivityId = other.labResultSensitivityId;
        this.labResultId = other.labResultId;
        this.agent = other.agent;
        this.sensitivity = other.sensitivity;
        this.reportingDatetime = other.reportingDatetime;
    }


    @Override
    public LabResultSensitivity copy() {
        return new LabResultSensitivity(this);
    }

    @Override
    public LabResultSensitivityAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabResultSensitivityAudit(this, validUntil, storedUntil);
    }
}
