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

    private String flowsheetId = "";

    /**
     * Numeric value, null if not set.
     */
    private Double numericValue;

    /**
     * String value, null if not set.
     */
    private String stringValue;

    /**
     * Comment, null if not set.
     */
    private String comment;

    /**
     * Result status has default value of {@link ResultStatus#SAVE}.
     */
    private ResultStatus resultStatus = ResultStatus.SAVE;

    private String unit = "";

    /**
     * Time of the observation.
     */
    private Instant observationTimeTaken;

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
    public Double getNumericValue() {
        return numericValue;
    }

    /**
     * Returns recorded string value.
     * @return {@link Flowsheet#stringValue}
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Returns recorded comment.
     * @return {@link Flowsheet#comment}
     */
    public String getComment() {
        return comment;
    }

    /**
     * Gets the result status.
     * @return {@link Flowsheet#resultStatus}
     */
    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    /**
     * Returns unit of the observation value.
     * @return String unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns time observation was taken.
     * @return Instant time taken
     */
    public Instant getObservationTimeTaken() {
        return observationTimeTaken;
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
     * Sets the flowsheet Identifier.
     * @param flowsheetId String value of flowsheet identifier
     */
    public void setFlowsheetId(String flowsheetId) {
        this.flowsheetId = flowsheetId;
    }

    /**
     * Sets the value as a number.
     * @param value {@link Flowsheet#numericValue}
     */
    public void setNumericValue(Double value) {
        this.numericValue = value;
    }

    /**
     * Sets the value as a string.
     * @param value {@link Flowsheet#stringValue}
     */
    public void setStringValue(String value) {
        this.stringValue = value;
    }

    /**
     * Sets the comment.
     * @param comment {@link Flowsheet#comment}
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Sets the result status {@link Flowsheet#resultStatus}.
     * @param resultStatus action to be taken when the interchange message is parsed.
     */
    public void setResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    /**
     * Sets the unit.
     * @param unit String unit for a flowsheet numeric value
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Sets the time observation was taken.
     * @param taken Instant time taken
     */
    public void setObservationTimeTaken(Instant taken) {
        this.observationTimeTaken = taken;
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
                && Objects.equals(observationTimeTaken, that.observationTimeTaken)
                && Objects.equals(getSourceSystem(), that.getSourceSystem());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mrn, visitNumber, flowsheetId, numericValue, stringValue, comment, unit, observationTimeTaken, getSourceSystem());
    }
}
