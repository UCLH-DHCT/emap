package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * VisitObservations represent discrete nurse (or machine) recoded observations
 * about patients at specific time points.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class VisitObservation extends TemporalCore<VisitObservation> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long    visitObservationId;
    private long    visitObservationDurableId;

    private long    visitObservationTypeId;

    private String  valueAsText;
    private double  valueAsReal;
    private String  unit;

    /**
     * The time this indvidiual observation was made.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant recordingDatetime;

    /**
     * The time the set of observations (observations are often taken in groups) is
     * associated with.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant panelDatetime;

    public VisitObservation() {}

    public VisitObservation(VisitObservation other) {
        super(other);

        this.visitObservationDurableId = other.visitObservationDurableId;

        this.visitObservationTypeId = other.visitObservationTypeId;

        this.valueAsText = other.valueAsText;
        this.valueAsReal = other.valueAsReal;

        this.unit = other.unit;

        this.recordingDatetime = other.recordingDatetime;
        this.panelDatetime = other.panelDatetime;
    }

    /**
     * @return the visitObservationId
     */
    public long getVisitObservationId() {
        return visitObservationId;
    }

    /**
     * @param visitObservationId the visitObservationId to set
     */
    public void setVisitObservationId(long visitObservationId) {
        this.visitObservationId = visitObservationId;
    }

    /**
     * @return the visitObservationDurableId
     */
    public long getVisitObservationDurableId() {
        return visitObservationDurableId;
    }

    /**
     * @param visitObservationDurableId the visitObservationDurableId to set
     */
    public void setVisitObservationDurableId(long visitObservationDurableId) {
        this.visitObservationDurableId = visitObservationDurableId;
    }

    /**
     * @return the visitObservationTypeId
     */
    public long getVisitObservationTypeId() {
        return visitObservationTypeId;
    }

    /**
     * @param visitObservationTypeId the visitObservationTypeId to set
     */
    public void setVisitObservationTypeId(long visitObservationTypeId) {
        this.visitObservationTypeId = visitObservationTypeId;
    }

    /**
     * @return the valueAsText
     */
    public String getValueAsText() {
        return valueAsText;
    }

    /**
     * @param valueAsText the valueAsText to set
     */
    public void setValueAsText(String valueAsText) {
        this.valueAsText = valueAsText;
    }

    /**
     * @return the valueAsReal
     */
    public double getValueAsReal() {
        return valueAsReal;
    }

    /**
     * @param valueAsReal the valueAsReal to set
     */
    public void setValueAsReal(double valueAsReal) {
        this.valueAsReal = valueAsReal;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the unit to set
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the recordingDatetime
     */
    public Instant getRecordingDatetime() {
        return recordingDatetime;
    }

    /**
     * @param recordingDatetime the recordingDatetime to set
     */
    public void setRecordingDatetime(Instant recordingDatetime) {
        this.recordingDatetime = recordingDatetime;
    }

    /**
     * @return the panelDatetime
     */
    public Instant getPanelDatetime() {
        return panelDatetime;
    }

    /**
     * @param panelDatetime the panelDatetime to set
     */
    public void setPanelDatetime(Instant panelDatetime) {
        this.panelDatetime = panelDatetime;
    }

    @Override
    public VisitObservation copy() {
        return new VisitObservation(this);
    }

}
