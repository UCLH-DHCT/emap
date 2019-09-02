package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CE;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.ED;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.datatype.NM;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;

/**
 * Represent a pathology result message.
 *
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class PathologyResult implements Serializable {
    protected static final Logger logger = LoggerFactory.getLogger(PathologyResult.class);

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
     * @return the local code for the particular test
     */
    public String getTestItemLocalCode() {
        return testItemLocalCode;
    }

    /**
     * @return the description for the particular test
     */
    public String getTestItemLocalDescription() {
        return testItemLocalDescription;
    }

    /**
     * @return the coding system (eg. WinPath)
     */
    public String getTestItemCodingSystem() {
        return testItemCodingSystem;
    }

    /**
     * @return the numerical value of the test (if numerical)
     */
    public Double getNumericValue() {
        return numericValue;
    }

    /**
     * Get the String representation of the result.
     *
     * @return the String representation of the results.
     */
    public String getStringValue() {
        return this.stringValue;
    }

    /**
     * @return the units for a numerical test
     */
    public String getUnits() {
        return units;
    }

    /**
     * @return the time the result was reported. Can differ within
     * a test battery if results are delivered bit by bit.
     */
    public Instant getResultTime() {
        return resultTime;
    }

    /**
     * @return the reference range for the numerical result
     */
    public String getReferenceRange() {
        return referenceRange;
    }

    /**
     * @return the result status. See HL7 Table 0085.
     */
    public String getResultStatus() {
        return resultStatus;
    }

    /**
     * @return the notes accompanying the result, if any
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @return all sensitivities
     */
    public List<PathologyOrder> getPathologySensitivities() {
        return pathologySensitivities;
    }

    /**
     * @return the sub-ID that links observations together
     */
    public String getObservationSubId() {
        return observationSubId;
    }

    /**
     * @return the Epic order number that this result relates to
     */
    public String getEpicCareOrderNumber() {
        return epicCareOrderNumber;
    }

    /**
     * @return local code of the isolate
     */
    public String getIsolateLocalCode() {
        return isolateLocalCode;
    }

    /**
     * @return local description of the isolate
     */
    public String getIsolateLocalDescription() {
        return isolateLocalDescription;
    }

    /**
     * @return coding system of the isolate (eg. WinPath)
     */
    public String getIsolateCodingSystem() {
        return isolateCodingSystem;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setTestItemLocalCode(String testItemLocalCode) {
        this.testItemLocalCode = testItemLocalCode;
    }

    public void setTestItemLocalDescription(String testItemLocalDescription) {
        this.testItemLocalDescription = testItemLocalDescription;
    }

    public void setTestItemCodingSystem(String testItemCodingSystem) {
        this.testItemCodingSystem = testItemCodingSystem;
    }

    public void setObservationSubId(String observationSubId) {
        this.observationSubId = observationSubId;
    }

    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public void setIsolateLocalCode(String isolateLocalCode) {
        this.isolateLocalCode = isolateLocalCode;
    }

    public void setIsolateLocalDescription(String isolateLocalDescription) {
        this.isolateLocalDescription = isolateLocalDescription;
    }

    public void setIsolateCodingSystem(String isolateCodingSystem) {
        this.isolateCodingSystem = isolateCodingSystem;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public void setResultTime(Instant resultTime) {
        this.resultTime = resultTime;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPathologySensitivities(List<PathologyOrder> pathologySensitivities) {
        this.pathologySensitivities = pathologySensitivities;
    }

    public void setEpicCareOrderNumber(String epicCareOrderNumber) {
        this.epicCareOrderNumber = epicCareOrderNumber;
    }
}
