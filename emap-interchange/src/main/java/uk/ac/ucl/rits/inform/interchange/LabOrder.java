package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The top level of the lab tree, the order. Only the interchange format
 * is declared here, for serialisation purposes. Builder classes (eg. HL7
 * parser) construct this class.
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabOrder extends EmapOperationMessage implements Serializable {
    private static final long serialVersionUID = -8476559759815762054L;

    private List<LabResult> labResults = new ArrayList<>();
    private String orderControlId;
    private String epicCareOrderNumber;
    private String labSpecimenNumber;
    private String specimenType;
    private Instant orderDateTime;
    private Instant sampleEnteredTime;
    private String labDepartment;
    private String orderStatus;
    private String resultStatus;
    private String orderType;
    private String mrn;

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
     * @return the EpicCare order number for this order
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

    /**
     * @return the lab number for this order (known as the accession number by Epic)
     */
    public String getLabSpecimenNumber() {
        return labSpecimenNumber;
    }

    /**
     * @param labSpecimenNumber the labSpecimenNumber to set
     */
    public void setLabSpecimenNumber(String labSpecimenNumber) {
        this.labSpecimenNumber = labSpecimenNumber;
    }

    /**
     * @return the lab number with an extra character appended (known as the OCS
     * number in WinPath)
     */
    public String getSpecimenType() {
        return specimenType;
    }

    /**
     * @param specimenType the labSpecimenNumberOCS to set
     */
    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    /**
     * @return when the sample was taken
     */
    public Instant getObservationDateTime() {
        return observationDateTime;
    }

    /**
     * @param observationDateTime the observationDateTime to set
     */
    public void setObservationDateTime(Instant observationDateTime) {
        this.observationDateTime = observationDateTime;
    }

    /**
     * @return the order control ID in the message
     */
    public String getOrderControlId() {
        return orderControlId;
    }

    /**
     * @param orderControlId the orderControlId to set
     */
    public void setOrderControlId(String orderControlId) {
        this.orderControlId = orderControlId;
    }

    /**
     * @return date the order was originally made
     */
    public Instant getOrderDateTime() {
        return orderDateTime;
    }

    /**
     * @param orderDateTime the orderDateTime to set
     */
    public void setOrderDateTime(Instant orderDateTime) {
        this.orderDateTime = orderDateTime;
    }

    /**
     * @return Order status (final, incomplete, etc.). A,CA,CM,DC,ER,HD,IP,RP,SC
     * (HL7 Table 0038)
     */
    public String getOrderStatus() {
        return orderStatus;
    }

    /**
     * @param orderStatus the orderStatus to set
     */
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    /**
     * @return the resultStatus
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
     * @return (patient) type for order (inpatient or outpatient)
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     * @param orderType the orderType to set
     */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /**
     * @return the HL7 field to indicate the test identifier of the parent order for
     * this order, if it has one. Arguably this shouldn't be stored in the
     * JSON as it's a temporary value we use for building the structure and
     * is HL7 specific.
     */
    public String getParentObservationIdentifier() {
        return parentObservationIdentifier;
    }

    /**
     * @param parentObservationIdentifier the parentObservationIdentifier to set
     */
    public void setParentObservationIdentifier(String parentObservationIdentifier) {
        this.parentObservationIdentifier = parentObservationIdentifier;
    }

    /**
     * @return the HL7 field to indicate the sub ID of the parent order for this
     * order, if it has one. Arguably this shouldn't be stored in the JSON
     * as it's a temporary value we use for building the structure and is
     * HL7 specific.
     */
    public String getParentSubId() {
        return parentSubId;
    }

    /**
     * @param parentSubId the parentSubId to set
     */
    public void setParentSubId(String parentSubId) {
        this.parentSubId = parentSubId;
    }

    /**
     * @return The results for this order (will be empty if constructed from an ORM
     * message)
     */
    public List<LabResult> getLabResults() {
        return labResults;
    }

    /**
     * @return int number of lab results in list
     */
    @JsonIgnore
    public int getNumLabResults() {
        return labResults.size();
    }

    /**
     * Add a LabResult to list.
     * @param result LabResult to add
     */
    public void addLabResult(LabResult result) {
        labResults.add(result);
    }

    /**
     * @param labResults the labResults to set
     */
    public void setLabResults(List<LabResult> labResults) {
        this.labResults = labResults;
    }

    /**
     * @return the requested date/time - how is this different to order time?
     */
    public Instant getRequestedDateTime() {
        return requestedDateTime;
    }

    /**
     * @param requestedDateTime the requestedDateTime to set
     */
    public void setRequestedDateTime(Instant requestedDateTime) {
        this.requestedDateTime = requestedDateTime;
    }

    /**
     * @return date the sample was entered onto WinPath
     */
    public Instant getSampleEnteredTime() {
        return sampleEnteredTime;
    }

    /**
     * @param sampleEnteredTime the sampleEnteredTime to set
     */
    public void setSampleEnteredTime(Instant sampleEnteredTime) {
        this.sampleEnteredTime = sampleEnteredTime;
    }

    /**
     * @return the labDepartment code
     */
    public String getLabDepartment() {
        return labDepartment;
    }

    /**
     * @param labDepartment the labDepartment code to set
     */
    public void setLabDepartment(String labDepartment) {
        this.labDepartment = labDepartment;
    }

    /**
     * @return the time the status of the results last changed
     */
    public Instant getStatusChangeTime() {
        return statusChangeTime;
    }

    /**
     * @param statusChangeTime the statusChangeTime to set
     */
    public void setStatusChangeTime(Instant statusChangeTime) {
        this.statusChangeTime = statusChangeTime;
    }

    /**
     * @return The local coding system in use (eg. WinPath)
     */
    public String getTestBatteryCodingSystem() {
        return testBatteryCodingSystem;
    }

    /**
     * @param testBatteryCodingSystem the testBatteryCodingSystem to set
     */
    public void setTestBatteryCodingSystem(String testBatteryCodingSystem) {
        this.testBatteryCodingSystem = testBatteryCodingSystem;
    }

    /**
     * @param testBatteryLocalCode the testBatteryLocalCode to set
     */
    public void setTestBatteryLocalCode(String testBatteryLocalCode) {
        this.testBatteryLocalCode = testBatteryLocalCode;
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
     * @param testBatteryLocalDescription the testBatteryLocalDescription to set
     */
    public void setTestBatteryLocalDescription(String testBatteryLocalDescription) {
        this.testBatteryLocalDescription = testBatteryLocalDescription;
    }

    /**
     * @return the patient's MRN
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param mrn the patient's MRN
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the visit number (CSN) of the patient
     */
    public String getVisitNumber() {
        return visitNumber;
    }

    /**
     * @param visitNumber the visitNumber to set
     */
    public void setVisitNumber(String visitNumber) {
        this.visitNumber = visitNumber;
    }

    /**
     * Call back to the processor so it knows what type this object is (ie. double
     * dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be
     *                                                 processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor)
            throws EmapOperationMessageProcessingException {
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
        LabOrder that = (LabOrder) o;
        return Objects.equals(orderControlId, that.orderControlId)
                && Objects.equals(labResults, that.labResults)
                && Objects.equals(epicCareOrderNumber, that.epicCareOrderNumber)
                && Objects.equals(labSpecimenNumber, that.labSpecimenNumber)
                && Objects.equals(specimenType, that.specimenType)
                && Objects.equals(orderDateTime, that.orderDateTime)
                && Objects.equals(sampleEnteredTime, that.sampleEnteredTime)
                && Objects.equals(labDepartment, that.labDepartment)
                && Objects.equals(orderStatus, that.orderStatus)
                && Objects.equals(resultStatus, that.resultStatus)
                && Objects.equals(orderType, that.orderType)
                && Objects.equals(mrn, that.mrn)
                && Objects.equals(visitNumber, that.visitNumber)
                && Objects.equals(requestedDateTime, that.requestedDateTime)
                && Objects.equals(observationDateTime, that.observationDateTime)
                && Objects.equals(testBatteryLocalCode, that.testBatteryLocalCode)
                && Objects.equals(testBatteryLocalDescription, that.testBatteryLocalDescription)
                && Objects.equals(testBatteryCodingSystem, that.testBatteryCodingSystem)
                && Objects.equals(statusChangeTime, that.statusChangeTime)
                && Objects.equals(parentObservationIdentifier, that.parentObservationIdentifier)
                && Objects.equals(parentSubId, that.parentSubId)
                && Objects.equals(getSourceSystem(), that.getSourceSystem());
    }

    @Override
    public int hashCode() {
        return Objects.hash(labResults, orderControlId, epicCareOrderNumber, labSpecimenNumber,
                specimenType, orderDateTime, sampleEnteredTime, labDepartment, orderStatus,
                resultStatus, orderType, mrn, visitNumber, requestedDateTime, observationDateTime,
                testBatteryLocalCode, testBatteryLocalDescription, testBatteryCodingSystem, statusChangeTime,
                parentObservationIdentifier, parentSubId, getSourceSystem());
    }

    @Override
    public String toString() {
        return String.format(new StringBuilder()
                        .append("\nLabOrder{labResults=%s, orderControlId='%s', epicCareOrderNumber='%s', ")
                        .append("labSpecimenNumber='%s', labSpecimenNumberOCS='%s', orderDateTime=%s, sampleEnteredTime=%s, labDepartment='%s', ")
                        .append("orderStatus='%s', resultStatus='%s', orderType='%s', mrn='%s', visitNumber='%s', requestedDateTime=%s, ")
                        .append("observationDateTime=%s, testBatteryLocalCode='%s', testBatteryLocalDescription='%s', ")
                        .append("testBatteryCodingSystem='%s', statusChangeTime=%s, parentObservationIdentifier='%s', parentSubId='%s', ")
                        .append("sourceSystem='%s'}")
                        .toString(),
                labResults, orderControlId, epicCareOrderNumber, labSpecimenNumber, specimenType,
                orderDateTime, sampleEnteredTime, labDepartment, orderStatus, resultStatus, orderType, mrn,
                visitNumber, requestedDateTime, observationDateTime, testBatteryLocalCode, testBatteryLocalDescription,
                testBatteryCodingSystem, statusChangeTime, parentObservationIdentifier, parentSubId, getSourceSystem());
    }
}
