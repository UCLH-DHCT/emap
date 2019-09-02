package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The top level of the pathology tree, the order.
 * Only the interchange format is declared here, for serialisation purposes.
 * Builder classes (eg. HL7 parser) construct this class.
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class PathologyOrder implements EmapOperationMessage, Serializable {
    private static final long serialVersionUID = -8476559759815762054L;

    protected static final Logger logger = LoggerFactory.getLogger(PathologyOrder.class);

    private List<PathologyResult> pathologyResults = new ArrayList<>();
    private String orderControlId;
    private String epicCareOrderNumber;
    private String labSpecimenNumber;
    private String labSpecimenNumberOCS;
    private Instant orderDateTime;
    private Instant sampleEnteredTime;
    private String orderStatus;
    private String orderType;
    private String visitNumber;
    private Instant requestedDateTime;
    private Instant observationDateTime;
    private String testBatteryLocalCode;
    private String testBatteryLocalDescription;
    private String testBatteryCodingSystem;
    private Instant statusChangeTime;

    private String parentObservationIdentifier;
    private String parentSubId;

    /**
     * @return the order control ID in the message
     */
    public String getOrderControlId() {
        return orderControlId;
    }

    /**
     * @return the EpicCare order number for this order
     */
    public String getEpicCareOrderNumber() {
        return epicCareOrderNumber;
    }

    /**
     * @return the lab number for this order (known as the accession number by Epic)
     */
    public String getLabSpecimenNumber() {
        return labSpecimenNumber;
    }

    /**
     * @return the lab number with an extra character appended (known as the OCS number in WinPath)
     */
    public String getLabSpecimenNumberOCS() {
        return labSpecimenNumberOCS;
    }

    /**
     * @return date the order was originally made
     */
    public Instant getOrderDateTime() {
        return orderDateTime;
    }

    /**
     * @return date the sample was entered onto WinPath
     */
    public Instant getSampleEnteredTime() {
        return sampleEnteredTime;
    }

    /**
     * @return (patient) type for order (inpatient or outpatient)
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     * @return the visit number (CSN) of the patient
     */
    public String getVisitNumber() {
        return visitNumber;
    }

    /**
     * @return The results for this order (will be empty if constructed from an ORM message)
     */
    public List<PathologyResult> getPathologyResults() {
        return pathologyResults;
    }

    /**
     * @return when the sample was taken
     */
    public Instant getObservationDateTime() {
        return observationDateTime;
    }

    /**
     * @return the local code (eg. WinPath code) for the test battery
     */
    public String getTestBatteryLocalCode() {
        return testBatteryLocalCode;
    }

    /**
     * @return the local description (eg. in WinPath) of the test battery
     */
    public String getTestBatteryLocalDescription() {
        return testBatteryLocalDescription;
    }

    /**
     * @return The local coding system in use (eg. WinPath)
     */
    public String getTestBatteryCodingSystem() {
        return testBatteryCodingSystem;
    }

    /**
     * @return the time the status of the results last changed
     */
    public Instant getStatusChangeTime() {
        return statusChangeTime;
    }

    /**
     * @return the requested date/time - how is this different to order time?
     */
    public Instant getRequestedDateTime() {
        return requestedDateTime;
    }

    /**
     * @return Order status (final, incomplete, etc.).
     * A,CA,CM,DC,ER,HD,IP,RP,SC (HL7 Table 0038)
     */
    public String getOrderStatus() {
        return orderStatus;
    }

    /**
     * @return the HL7 field to indicate the test identifier of the parent order for
     *         this order, if it has one. Arguably this shouldn't be stored in the
     *         JSON as it's a temporary value we use for building the structure and
     *         is HL7 specific.
     */
    public String getParentObservationIdentifier() {
        return parentObservationIdentifier;
    }

    /**
     * @return the HL7 field to indicate the sub ID of the parent order for this
     *         order, if it has one. Arguably this shouldn't be stored in the JSON
     *         as it's a temporary value we use for building the structure and is
     *         HL7 specific.
     */
    public String getParentSubId() {
        return parentSubId;
    }

    public void setPathologyResults(List<PathologyResult> pathologyResults) {
        this.pathologyResults = pathologyResults;
    }

    public void setOrderControlId(String orderControlId) {
        this.orderControlId = orderControlId;
    }

    public void setEpicCareOrderNumber(String epicCareOrderNumber) {
        this.epicCareOrderNumber = epicCareOrderNumber;
    }

    public void setLabSpecimenNumber(String labSpecimenNumber) {
        this.labSpecimenNumber = labSpecimenNumber;
    }

    public void setLabSpecimenNumberOCS(String labSpecimenNumberOCS) {
        this.labSpecimenNumberOCS = labSpecimenNumberOCS;
    }

    public void setOrderDateTime(Instant orderDateTime) {
        this.orderDateTime = orderDateTime;
    }

    public void setSampleEnteredTime(Instant sampleEnteredTime) {
        this.sampleEnteredTime = sampleEnteredTime;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public void setVisitNumber(String visitNumber) {
        this.visitNumber = visitNumber;
    }

    public void setRequestedDateTime(Instant requestedDateTime) {
        this.requestedDateTime = requestedDateTime;
    }

    public void setObservationDateTime(Instant observationDateTime) {
        this.observationDateTime = observationDateTime;
    }

    public void setTestBatteryLocalCode(String testBatteryLocalCode) {
        this.testBatteryLocalCode = testBatteryLocalCode;
    }

    public void setTestBatteryLocalDescription(String testBatteryLocalDescription) {
        this.testBatteryLocalDescription = testBatteryLocalDescription;
    }

    public void setTestBatteryCodingSystem(String testBatteryCodingSystem) {
        this.testBatteryCodingSystem = testBatteryCodingSystem;
    }

    public void setStatusChangeTime(Instant statusChangeTime) {
        this.statusChangeTime = statusChangeTime;
    }

    public void setParentObservationIdentifier(String parentObservationIdentifier) {
        this.parentObservationIdentifier = parentObservationIdentifier;
    }

    public void setParentSubId(String parentSubId) {
        this.parentSubId = parentSubId;
    }

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) {
        processor.processMessage(this);
    }
}
