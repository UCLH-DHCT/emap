package uk.ac.ucl.rits.inform.informdb.labs;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * Sensitivites show the affect of specific agents on isolates from cultures.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class LabResultSensitivity extends TemporalCore<LabResultSensitivity> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long    labResultSensitivityId;
    private long    labResultSensitivityDurableId;

    private long    labResultDurableId;

    /**
     * The chemical (often antibiotic) this applies too.
     */
    private String  agent;
    private String  sensitivity;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant reportingDatetime;

    public LabResultSensitivity() {}

    public LabResultSensitivity(LabResultSensitivity other) {
        super(other);

        this.labResultSensitivityDurableId = other.labResultSensitivityDurableId;

        this.labResultDurableId = other.labResultDurableId;
        this.agent = other.agent;
        this.sensitivity = other.sensitivity;
        this.reportingDatetime = other.reportingDatetime;
    }

    @Override
    public LabResultSensitivity copy() {
        return new LabResultSensitivity(this);
    }

}
