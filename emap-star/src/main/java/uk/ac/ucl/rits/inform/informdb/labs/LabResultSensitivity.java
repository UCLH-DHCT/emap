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

    /**
     * @return the labResultSensitivityId
     */
    public long getLabResultSensitivityId() {
        return labResultSensitivityId;
    }

    /**
     * @param labResultSensitivityId the labResultSensitivityId to set
     */
    public void setLabResultSensitivityId(long labResultSensitivityId) {
        this.labResultSensitivityId = labResultSensitivityId;
    }

    /**
     * @return the labResultSensitivityDurableId
     */
    public long getLabResultSensitivityDurableId() {
        return labResultSensitivityDurableId;
    }

    /**
     * @param labResultSensitivityDurableId the labResultSensitivityDurableId to set
     */
    public void setLabResultSensitivityDurableId(long labResultSensitivityDurableId) {
        this.labResultSensitivityDurableId = labResultSensitivityDurableId;
    }

    /**
     * @return the labResultDurableId
     */
    public long getLabResultDurableId() {
        return labResultDurableId;
    }

    /**
     * @param labResultDurableId the labResultDurableId to set
     */
    public void setLabResultDurableId(long labResultDurableId) {
        this.labResultDurableId = labResultDurableId;
    }

    /**
     * @return the agent
     */
    public String getAgent() {
        return agent;
    }

    /**
     * @param agent the agent to set
     */
    public void setAgent(String agent) {
        this.agent = agent;
    }

    /**
     * @return the sensitivity
     */
    public String getSensitivity() {
        return sensitivity;
    }

    /**
     * @param sensitivity the sensitivity to set
     */
    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    /**
     * @return the reportingDatetime
     */
    public Instant getReportingDatetime() {
        return reportingDatetime;
    }

    /**
     * @param reportingDatetime the reportingDatetime to set
     */
    public void setReportingDatetime(Instant reportingDatetime) {
        this.reportingDatetime = reportingDatetime;
    }

    @Override
    public LabResultSensitivity copy() {
        return new LabResultSensitivity(this);
    }

}
