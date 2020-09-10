package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * An interchange message describing patient movements or info. Closely corresponds
 * to the HL7 ADT message type.
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class OldAdtMessage extends EmapOperationMessage implements Serializable {
    private static final long serialVersionUID = -3352058605994102510L;

    /**
     * Only expecting the deserialiser to call this.
     */
    public OldAdtMessage() {
    }

    private AdtOperationType operationType;
    private Instant recordedDateTime;
    private String eventReasonCode;
    private Instant eventOccurredDateTime;
    private String operatorId;
    private Instant admissionDateTime;
    private String admitSource;
    private String currentBed;
    private String currentRoomCode;
    private String currentWardCode;
    private Instant dischargeDateTime;
    private String dischargeDisposition;
    private String dischargeLocation;
    private String ethnicGroup;
    private String fullLocationString;
    private String hospitalService;
    private String mrn;
    private String mergedPatientId;
    private String nhsNumber;
    private Instant patientBirthDate;
    private String patientClass;
    private Instant patientDeathDateTime;
    private Boolean patientDeathIndicator;
    private String patientFamilyName;
    private String patientFullName;
    private String patientGivenName;
    private String patientMiddleName;
    private String patientReligion;
    private String patientSex;
    private String patientTitle;
    private String patientType;
    private String patientZipOrPostalCode;
    private String visitNumber;

    /**
     * @return the operationType
     */
    public AdtOperationType getOperationType() {
        return operationType;
    }

    /**
     * @param operationType the operationType to set
     */
    public void setOperationType(AdtOperationType operationType) {
        this.operationType = operationType;
    }

    /**
     * @return the recordedDateTime
     */
    public Instant getRecordedDateTime() {
        return recordedDateTime;
    }

    /**
     * @param recordedDateTime the recordedDateTime to set
     */
    public void setRecordedDateTime(Instant recordedDateTime) {
        this.recordedDateTime = recordedDateTime;
    }

    /**
     * @return the eventReasonCode
     */
    public String getEventReasonCode() {
        return eventReasonCode;
    }

    /**
     * @param eventReasonCode the eventReasonCode to set
     */
    public void setEventReasonCode(String eventReasonCode) {
        this.eventReasonCode = eventReasonCode;
    }

    /**
     * @return the eventOccurredDateTime
     */
    public Instant getEventOccurredDateTime() {
        return eventOccurredDateTime;
    }

    /**
     * @param eventOccurredDateTime the eventOccurredDateTime to set
     */
    public void setEventOccurredDateTime(Instant eventOccurredDateTime) {
        this.eventOccurredDateTime = eventOccurredDateTime;
    }

    /**
     * @return the admissionDateTime
     */
    public Instant getAdmissionDateTime() {
        return admissionDateTime;
    }

    /**
     * @param admissionDateTime the admissionDateTime to set
     */
    public void setAdmissionDateTime(Instant admissionDateTime) {
        this.admissionDateTime = admissionDateTime;
    }

    /**
     * @return the admitSource
     */
    public String getAdmitSource() {
        return admitSource;
    }

    /**
     * @param admitSource the admitSource to set
     */
    public void setAdmitSource(String admitSource) {
        this.admitSource = admitSource;
    }

    /**
     * @return the currentBed
     */
    public String getCurrentBed() {
        return currentBed;
    }

    /**
     * @param currentBed the currentBed to set
     */
    public void setCurrentBed(String currentBed) {
        this.currentBed = currentBed;
    }

    /**
     * @return the currentRoomCode
     */
    public String getCurrentRoomCode() {
        return currentRoomCode;
    }

    /**
     * @param currentRoomCode the currentRoomCode to set
     */
    public void setCurrentRoomCode(String currentRoomCode) {
        this.currentRoomCode = currentRoomCode;
    }

    /**
     * @return the currentWardCode
     */
    public String getCurrentWardCode() {
        return currentWardCode;
    }

    /**
     * @param currentWardCode the currentWardCode to set
     */
    public void setCurrentWardCode(String currentWardCode) {
        this.currentWardCode = currentWardCode;
    }

    /**
     * @return the dischargeDateTime
     */
    public Instant getDischargeDateTime() {
        return dischargeDateTime;
    }

    /**
     * @param dischargeDateTime the dischargeDateTime to set
     */
    public void setDischargeDateTime(Instant dischargeDateTime) {
        this.dischargeDateTime = dischargeDateTime;
    }

    /**
     * @return the dischargeDisposition
     */
    public String getDischargeDisposition() {
        return dischargeDisposition;
    }

    /**
     * @param dischargeDisposition the dischargeDisposition to set
     */
    public void setDischargeDisposition(String dischargeDisposition) {
        this.dischargeDisposition = dischargeDisposition;
    }

    /**
     * @return the dischargeLocation
     */
    public String getDischargeLocation() {
        return dischargeLocation;
    }

    /**
     * @param dischargeLocation the dischargeLocation to set
     */
    public void setDischargeLocation(String dischargeLocation) {
        this.dischargeLocation = dischargeLocation;
    }

    /**
     * @return the ethnicGroup
     */
    public String getEthnicGroup() {
        return ethnicGroup;
    }

    /**
     * @param ethnicGroup the ethnicGroup to set
     */
    public void setEthnicGroup(String ethnicGroup) {
        this.ethnicGroup = ethnicGroup;
    }

    /**
     * @return the fullLocationString
     */
    public String getFullLocationString() {
        return fullLocationString;
    }

    /**
     * @param fullLocationString the fullLocationString to set
     */
    public void setFullLocationString(String fullLocationString) {
        this.fullLocationString = fullLocationString;
    }

    /**
     * @return the hospitalService
     */
    public String getHospitalService() {
        return hospitalService;
    }

    /**
     * @param hospitalService the hospitalService to set
     */
    public void setHospitalService(String hospitalService) {
        this.hospitalService = hospitalService;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the mergedPatientId
     */
    public String getMergedPatientId() {
        return mergedPatientId;
    }

    /**
     * @param mergedPatientId the mergedPatientId to set
     */
    public void setMergedPatientId(String mergedPatientId) {
        this.mergedPatientId = mergedPatientId;
    }

    /**
     * @return the nhsNumber
     */
    public String getNhsNumber() {
        return nhsNumber;
    }

    /**
     * @param nhsNumber the nhsNumber to set
     */
    public void setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    /**
     * @return the operatorId - ie. person logged into the EHRS at the time this fact was entered.
     */
    public String getOperatorId() {
        return operatorId;
    }

    /**
     * @param operatorId the operatorId to set
     */
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    /**
     * @return the patientBirthDate
     */
    public Instant getPatientBirthDate() {
        return patientBirthDate;
    }

    /**
     * @param patientBirthDate the patientBirthDate to set
     */
    public void setPatientBirthDate(Instant patientBirthDate) {
        this.patientBirthDate = patientBirthDate;
    }

    /**
     * @return the patientClass
     */
    public String getPatientClass() {
        return patientClass;
    }

    /**
     * @param patientClass the patientClass to set
     */
    public void setPatientClass(String patientClass) {
        this.patientClass = patientClass;
    }

    /**
     * @return the patientDeathDateTime
     */
    public Instant getPatientDeathDateTime() {
        return patientDeathDateTime;
    }

    /**
     * @param patientDeathDateTime the patientDeathDateTime to set
     */
    public void setPatientDeathDateTime(Instant patientDeathDateTime) {
        this.patientDeathDateTime = patientDeathDateTime;
    }

    /**
     * @return the patientDeathIndicator
     */
    public Boolean getPatientDeathIndicator() {
        return patientDeathIndicator;
    }

    /**
     * @param patientDeathIndicator the patientDeathIndicator to set
     */
    public void setPatientDeathIndicator(Boolean patientDeathIndicator) {
        this.patientDeathIndicator = patientDeathIndicator;
    }

    /**
     * @return the patientFamilyName
     */
    public String getPatientFamilyName() {
        return patientFamilyName;
    }

    /**
     * @param patientFamilyName the patientFamilyName to set
     */
    public void setPatientFamilyName(String patientFamilyName) {
        this.patientFamilyName = patientFamilyName;
    }

    /**
     * @return the patientFullName
     */
    public String getPatientFullName() {
        return patientFullName;
    }

    /**
     * @param patientFullName the patientFullName to set
     */
    public void setPatientFullName(String patientFullName) {
        this.patientFullName = patientFullName;
    }

    /**
     * @return the patientGivenName
     */
    public String getPatientGivenName() {
        return patientGivenName;
    }

    /**
     * @param patientGivenName the patientGivenName to set
     */
    public void setPatientGivenName(String patientGivenName) {
        this.patientGivenName = patientGivenName;
    }

    /**
     * @return the patientMiddleName
     */
    public String getPatientMiddleName() {
        return patientMiddleName;
    }

    /**
     * @param patientMiddleName the patientMiddleName to set
     */
    public void setPatientMiddleName(String patientMiddleName) {
        this.patientMiddleName = patientMiddleName;
    }

    /**
     * @return the patientReligion
     */
    public String getPatientReligion() {
        return patientReligion;
    }

    /**
     * @param patientReligion the patientReligion to set
     */
    public void setPatientReligion(String patientReligion) {
        this.patientReligion = patientReligion;
    }

    /**
     * @return the patientSex
     */
    public String getPatientSex() {
        return patientSex;
    }

    /**
     * @param patientSex the patientSex to set
     */
    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    /**
     * @return the patientTitle
     */
    public String getPatientTitle() {
        return patientTitle;
    }

    /**
     * @param patientTitle the patientTitle to set
     */
    public void setPatientTitle(String patientTitle) {
        this.patientTitle = patientTitle;
    }

    /**
     * @return the patientType
     */
    public String getPatientType() {
        return patientType;
    }

    /**
     * @param patientType the patientType to set
     */
    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    /**
     * @return the patientZipOrPostalCode
     */
    public String getPatientZipOrPostalCode() {
        return patientZipOrPostalCode;
    }

    /**
     * @param patientZipOrPostalCode the patientZipOrPostalCode to set
     */
    public void setPatientZipOrPostalCode(String patientZipOrPostalCode) {
        this.patientZipOrPostalCode = patientZipOrPostalCode;
    }

    /**
     * @return the visitNumber
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

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }

    /**
     * Identify this as an ADT message + its subtype.
     * @return the message type
     */
    @Override
    public String getMessageType() {
        return "Adt:" + getOperationType().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OldAdtMessage that = (OldAdtMessage) o;
        return Objects.equals(recordedDateTime, that.recordedDateTime)
                && operationType == that.operationType
                && Objects.equals(eventReasonCode, that.eventReasonCode)
                && Objects.equals(eventOccurredDateTime, that.eventOccurredDateTime)
                && Objects.equals(operatorId, that.operatorId)
                && Objects.equals(admissionDateTime, that.admissionDateTime)
                && Objects.equals(admitSource, that.admitSource)
                && Objects.equals(currentBed, that.currentBed)
                && Objects.equals(currentRoomCode, that.currentRoomCode)
                && Objects.equals(currentWardCode, that.currentWardCode)
                && Objects.equals(dischargeDateTime, that.dischargeDateTime)
                && Objects.equals(dischargeDisposition, that.dischargeDisposition)
                && Objects.equals(dischargeLocation, that.dischargeLocation)
                && Objects.equals(ethnicGroup, that.ethnicGroup)
                && Objects.equals(fullLocationString, that.fullLocationString)
                && Objects.equals(hospitalService, that.hospitalService)
                && Objects.equals(mrn, that.mrn)
                && Objects.equals(mergedPatientId, that.mergedPatientId)
                && Objects.equals(nhsNumber, that.nhsNumber)
                && Objects.equals(patientBirthDate, that.patientBirthDate)
                && Objects.equals(patientClass, that.patientClass)
                && Objects.equals(patientDeathDateTime, that.patientDeathDateTime)
                && Objects.equals(patientDeathIndicator, that.patientDeathIndicator)
                && Objects.equals(patientFamilyName, that.patientFamilyName)
                && Objects.equals(patientFullName, that.patientFullName)
                && Objects.equals(patientGivenName, that.patientGivenName)
                && Objects.equals(patientMiddleName, that.patientMiddleName)
                && Objects.equals(patientReligion, that.patientReligion)
                && Objects.equals(patientSex, that.patientSex)
                && Objects.equals(patientTitle, that.patientTitle)
                && Objects.equals(patientType, that.patientType)
                && Objects.equals(patientZipOrPostalCode, that.patientZipOrPostalCode)
                && Objects.equals(visitNumber, that.visitNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationType, recordedDateTime, eventReasonCode, eventOccurredDateTime, operatorId,
                admissionDateTime, admitSource, currentBed, currentRoomCode, currentWardCode, dischargeDateTime,
                dischargeDisposition, dischargeLocation, ethnicGroup, fullLocationString, hospitalService, mrn,
                mergedPatientId, nhsNumber, patientBirthDate, patientClass, patientDeathDateTime, patientDeathIndicator,
                patientFamilyName, patientFullName, patientGivenName, patientMiddleName, patientReligion, patientSex,
                patientTitle, patientType, patientZipOrPostalCode, visitNumber);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AdtMessage{");
        sb.append("operationType=").append(operationType);
        sb.append(", recordedDateTime=").append(recordedDateTime);
        sb.append(", eventReasonCode='").append(eventReasonCode).append('\'');
        sb.append(", eventOccurredDateTime=").append(eventOccurredDateTime);
        sb.append(", operatorId='").append(operatorId).append('\'');
        sb.append(", admissionDateTime=").append(admissionDateTime);
        sb.append(", admitSource='").append(admitSource).append('\'');
        sb.append(", currentBed='").append(currentBed).append('\'');
        sb.append(", currentRoomCode='").append(currentRoomCode).append('\'');
        sb.append(", currentWardCode='").append(currentWardCode).append('\'');
        sb.append(", dischargeDateTime=").append(dischargeDateTime);
        sb.append(", dischargeDisposition='").append(dischargeDisposition).append('\'');
        sb.append(", dischargeLocation='").append(dischargeLocation).append('\'');
        sb.append(", ethnicGroup='").append(ethnicGroup).append('\'');
        sb.append(", fullLocationString='").append(fullLocationString).append('\'');
        sb.append(", hospitalService='").append(hospitalService).append('\'');
        sb.append(", mrn='").append(mrn).append('\'');
        sb.append(", mergedPatientId='").append(mergedPatientId).append('\'');
        sb.append(", nhsNumber='").append(nhsNumber).append('\'');
        sb.append(", patientBirthDate=").append(patientBirthDate);
        sb.append(", patientClass='").append(patientClass).append('\'');
        sb.append(", patientDeathDateTime=").append(patientDeathDateTime);
        sb.append(", patientDeathIndicator=").append(patientDeathIndicator);
        sb.append(", patientFamilyName='").append(patientFamilyName).append('\'');
        sb.append(", patientFullName='").append(patientFullName).append('\'');
        sb.append(", patientGivenName='").append(patientGivenName).append('\'');
        sb.append(", patientMiddleName='").append(patientMiddleName).append('\'');
        sb.append(", patientReligion='").append(patientReligion).append('\'');
        sb.append(", patientSex='").append(patientSex).append('\'');
        sb.append(", patientTitle='").append(patientTitle).append('\'');
        sb.append(", patientType='").append(patientType).append('\'');
        sb.append(", patientZipOrPostalCode='").append(patientZipOrPostalCode).append('\'');
        sb.append(", visitNumber='").append(visitNumber).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
