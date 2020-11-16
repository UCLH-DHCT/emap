package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Objects;

/**
 * Represent a flowsheet message.
 * @author Sarah Keating & Stef Piatek
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Flowsheet extends EmapOperationMessage {
    private static final long serialVersionUID = -6678756549815762054L;

    private String mrn = "";

    private String visitNumber = "";

    /**
     * Application that the message is sent from (e.g. caboodle or EPIC)
     */
    private String sourceApplication = "";

    /**
     * Identifier used by caboodle/EPIC for the flowsheet.
     */
    private String flowsheetId = "";

    /**
     * Numeric value.
     */
    private Hl7Value<Double> numericValue = Hl7Value.unknown();

    /**
     * String value.
     */
    private Hl7Value<String> stringValue = Hl7Value.unknown();

    /**
     * Comment.
     */
    private Hl7Value<String> comment = Hl7Value.unknown();

    /**
     * Unit of numeric value.
     */
    private Hl7Value<String> unit = Hl7Value.unknown();

    /**
     * Time of the observation (can be the creation or update time).
     */
    private Instant observationTime;

    /**
     * Time that the panel of observations were created.
     */
    private Instant panelTime;

    /**
     * Returns MRN .
     * @return String mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * Returns visitNumber .
     * @return String visitNumber
     */
    public String getVisitNumber() {
        return visitNumber;
    }

    /**
     * Returns flowsheet Identifier e.g. Caboodle$1234.
     * @return String flowsheet identifier
     */
    public String getFlowsheetId() {
        return flowsheetId;
    }

    /**
     * Returns recorded numeric value.
     * @return {@link Flowsheet#numericValue}
     */
    public Hl7Value<Double> getNumericValue() {
        return numericValue;
    }

    /**
     * Returns recorded string value.
     * @return {@link Flowsheet#stringValue}
     */
    public Hl7Value<String> getStringValue() {
        return stringValue;
    }

    /**
     * Returns recorded comment.
     * @return {@link Flowsheet#comment}
     */
    public Hl7Value<String> getComment() {
        return comment;
    }

    /**
     * @return {@link Flowsheet#unit}
     */
    public Hl7Value<String> getUnit() {
        return unit;
    }

    /**
     * @return {@link Flowsheet#observationTime}
     */
    public Instant getObservationTime() {
        return observationTime;
    }

    /**
     * @return {@link Flowsheet#panelTime}
     */
    public Instant getPanelTime() {
        return panelTime;
    }

    /**
     * @return {@link Flowsheet#sourceApplication}
     */
    public String getSourceApplication() {
        return sourceApplication;
    }

    /**
     * Sets the MRN.
     * @param mrn String value of MRN
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * Sets the visitNumber.
     * @param visitNumber String value of visitNumber
     */
    public void setVisitNumber(String visitNumber) {
        this.visitNumber = visitNumber;
    }

    /**
     * @param flowsheetId {@link Flowsheet#flowsheetId}
     */
    public void setFlowsheetId(String flowsheetId) {
        this.flowsheetId = flowsheetId;
    }

    /**
     * @param value {@link Flowsheet#numericValue}
     */
    public void setNumericValue(Hl7Value<Double> value) {
        this.numericValue = value;
    }

    /**
     * @param value {@link Flowsheet#stringValue}
     */
    public void setStringValue(Hl7Value<String> value) {
        this.stringValue = value;
    }

    /**
     * @param comment {@link Flowsheet#comment}
     */
    public void setComment(Hl7Value<String> comment) {
        this.comment = comment;
    }

    /**
     * @param unit {@link Flowsheet#unit}
     */
    public void setUnit(Hl7Value<String> unit) {
        this.unit = unit;
    }

    /**
     * @param panelTime {@link Flowsheet#panelTime}
     */
    public void setPanelTime(Instant panelTime) {
        this.panelTime = panelTime;
    }

    /**
     * @param observationTime {@link Flowsheet#observationTime}
     */
    public void setObservationTime(Instant observationTime) {
        this.observationTime = observationTime;
    }

    /**
     * @param sourceApplication {@link Flowsheet#sourceApplication}
     */
    public void setSourceApplication(String sourceApplication) {
        this.sourceApplication = sourceApplication;
    }

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Flowsheet that = (Flowsheet) o;
        return Objects.equals(mrn, that.mrn)
                && Objects.equals(flowsheetId, that.flowsheetId)
                && Objects.equals(visitNumber, that.visitNumber)
                && Objects.equals(numericValue, that.numericValue)
                && Objects.equals(stringValue, that.stringValue)
                && Objects.equals(comment, that.comment)
                && Objects.equals(unit, that.unit)
                && Objects.equals(observationTime, that.observationTime)
                && Objects.equals(getSourceSystem(), that.getSourceSystem());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mrn, visitNumber, flowsheetId, numericValue, stringValue, comment, unit, observationTime, getSourceSystem());
    }


}
