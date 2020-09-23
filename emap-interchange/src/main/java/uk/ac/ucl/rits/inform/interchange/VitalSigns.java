package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Represent a vital signs message.
 * @author Sarah Keating
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class VitalSigns extends EmapOperationMessage implements Serializable {
    private static final long serialVersionUID = -6678756549815762054L;

    private String mrn = "";

    private String visitNumber = "";

    private String vitalSignIdentifier = "";

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
     * Returns vital sign Identifier e.g. Caboodle$1234.
     * @return String vital sign identifier
     */
    public String getVitalSignIdentifier() {
        return vitalSignIdentifier;
    }

    /**
     * Returns recorded numeric value.
     * @return {@link VitalSigns#numericValue}
     */
    public Double getNumericValue() {
        return numericValue;
    }

    /**
     * Returns recorded string value.
     * @return {@link VitalSigns#stringValue}
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Returns recorded comment.
     * @return {@link VitalSigns#comment}
     */
    public String getComment() {
        return comment;
    }

    /**
     * Gets the result status.
     * @return {@link VitalSigns#resultStatus}
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
     * Sets the vital sign Identifier.
     * @param vitalSignIdentifier String value of vital sign identifier
     */
    public void setVitalSignIdentifier(String vitalSignIdentifier) {
        this.vitalSignIdentifier = vitalSignIdentifier;
    }

    /**
     * Sets the value as a number.
     * @param value {@link VitalSigns#numericValue}
     */
    public void setNumericValue(Double value) {
        this.numericValue = value;
    }

    /**
     * Sets the value as a string.
     * @param value {@link VitalSigns#stringValue}
     */
    public void setStringValue(String value) {
        this.stringValue = value;
    }

    /**
     * Sets the comment.
     * @param comment {@link VitalSigns#comment}
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Sets the result status {@link VitalSigns#resultStatus}.
     * @param resultStatus action to be taken when the interchange message is parsed.
     */
    public void setResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    /**
     * Sets the unit.
     * @param unit String unit of vital sign numeric value
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
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VitalSigns that = (VitalSigns) o;
        return Objects.equals(mrn, that.mrn)
                && Objects.equals(vitalSignIdentifier, that.vitalSignIdentifier)
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
        return Objects.hash(mrn, visitNumber, vitalSignIdentifier, numericValue, stringValue, comment, unit, observationTimeTaken, getSourceSystem());
    }
}
