package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represent a pathology result. Note that this doesn't implement
 * EmapOperationMessage because it's not a message type
 * by itself, it is owned by a message type (PathologyOrder).
 *
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class PathologyResult implements Serializable {
    private String valueType = "";

    private String testItemLocalCode = "";
    private String testItemLocalDescription = "";
    private String testItemCodingSystem = "";

    private String observationSubId = "";
    private Double numericValue;
    private String stringValue = "";

    private String isolateLocalCode = "";
    private String isolateLocalDescription = "";
    private String isolateCodingSystem = "";

    private String units = "";
    private String referenceRange = "";
    private String abnormalFlags = "";
    private String resultStatus = "";

    private Instant resultTime;
    private String notes = "";

    /**
     * A sensitivity is just a nested pathology order with results.
     * HL7 has fields for working out parentage.
     * PathologySensitivity type can probably go away.
     */
    private List<PathologyOrder> pathologySensitivities = new ArrayList<>();

    private String epicCareOrderNumber = "";

    /**
     * @return value type for the observation line (eg. NM)
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * @param valueType the valueType to set
     */
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    /**
     * @return the local code for the particular test
     */
    public String getTestItemLocalCode() {
        return testItemLocalCode;
    }

    /**
     * @param testItemLocalCode the testItemLocalCode to set
     */
    public void setTestItemLocalCode(String testItemLocalCode) {
        this.testItemLocalCode = testItemLocalCode;
    }

    /**
     * @return the description for the particular test
     */
    public String getTestItemLocalDescription() {
        return testItemLocalDescription;
    }

    /**
     * @param testItemLocalDescription the testItemLocalDescription to set
     */
    public void setTestItemLocalDescription(String testItemLocalDescription) {
        this.testItemLocalDescription = testItemLocalDescription;
    }

    /**
     * @return the coding system (eg. WinPath)
     */
    public String getTestItemCodingSystem() {
        return testItemCodingSystem;
    }

    /**
     * @param testItemCodingSystem the testItemCodingSystem to set
     */
    public void setTestItemCodingSystem(String testItemCodingSystem) {
        this.testItemCodingSystem = testItemCodingSystem;
    }

    /**
     * @return the sub-ID that links observations together
     */
    public String getObservationSubId() {
        return observationSubId;
    }

    /**
     * @param observationSubId the observationSubId to set
     */
    public void setObservationSubId(String observationSubId) {
        this.observationSubId = observationSubId;
    }

    /**
     * @return the numerical value of the test (if numerical)
     */
    public Double getNumericValue() {
        return numericValue;
    }

    /**
     * @param numericValue the numericValue to set
     */
    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }

    /**
     * @return the String representation of the results.
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * @param stringValue the stringValue to set
     */
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * @return local code of the isolate
     */
    public String getIsolateLocalCode() {
        return isolateLocalCode;
    }

    /**
     * @param isolateLocalCode the isolateLocalCode to set
     */
    public void setIsolateLocalCode(String isolateLocalCode) {
        this.isolateLocalCode = isolateLocalCode;
    }

    /**
     * @return local description of the isolate
     */
    public String getIsolateLocalDescription() {
        return isolateLocalDescription;
    }

    /**
     * @param isolateLocalDescription the isolateLocalDescription to set
     */
    public void setIsolateLocalDescription(String isolateLocalDescription) {
        this.isolateLocalDescription = isolateLocalDescription;
    }

    /**
     * @return coding system of the isolate (eg. WinPath)
     */
    public String getIsolateCodingSystem() {
        return isolateCodingSystem;
    }

    /**
     * @param isolateCodingSystem the isolateCodingSystem to set
     */
    public void setIsolateCodingSystem(String isolateCodingSystem) {
        this.isolateCodingSystem = isolateCodingSystem;
    }

    /**
     * @return the units for a numerical test
     */
    public String getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * @return the reference range for the numerical result
     */
    public String getReferenceRange() {
        return referenceRange;
    }

    /**
     * @param referenceRange the referenceRange to set
     */
    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    /**
     * @return the abnormal flags, concatenated together
     */
    public String getAbnormalFlags() {
        return abnormalFlags;
    }

    /**
     * @param abnormalFlags the abnormal flags, concatenated together
     */
    public void setAbnormalFlags(String abnormalFlags) {
        this.abnormalFlags = abnormalFlags;
    }

    /**
     * @return the result status. See HL7 Table 0085.
     */
    public String getResultStatus() {
        return resultStatus;
    }

    /**
     * @param resultStatus the resultStatus to set
     */
    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    /**
     * @return the time the result was reported. Can differ within
     * a test battery if results are delivered bit by bit.
     */
    public Instant getResultTime() {
        return resultTime;
    }

    /**
     * @param resultTime the resultTime to set
     */
    public void setResultTime(Instant resultTime) {
        this.resultTime = resultTime;
    }

    /**
     * @return the notes accompanying the result, if any
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * @return all sensitivities
     */
    public List<PathologyOrder> getPathologySensitivities() {
        return pathologySensitivities;
    }

    /**
     * @param pathologySensitivities the pathologySensitivities to set
     */
    public void setPathologySensitivities(List<PathologyOrder> pathologySensitivities) {
        this.pathologySensitivities = pathologySensitivities;
    }

    /**
     * @return the Epic order number that this result relates to
     */
    public String getEpicCareOrderNumber() {
        return epicCareOrderNumber;
    }

    /**
     * @param epicCareOrderNumber the epicCareOrderNumber to set
     */
    public void setEpicCareOrderNumber(String epicCareOrderNumber) {
        this.epicCareOrderNumber = epicCareOrderNumber;
    }
}
