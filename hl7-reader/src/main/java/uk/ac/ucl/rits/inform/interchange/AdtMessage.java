package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.ac.ucl.rits.inform.datasources.ids.AdtOperationType;

/**
 * An interchange message describing patient movements or info. Closely corresponds
 * to the HL7 ADT message type.
 *
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class AdtMessage implements EmapOperationMessage, Serializable {
    private static final long serialVersionUID = -3352058605994102510L;

    /**
     * Only expecting the deserialiser to call this.
     */
    public AdtMessage() {
    }

    private AdtOperationType operationType;
    private Instant recordedDateTime;
    private String eventReasonCode;
    private Instant eventOccurredDateTime;
    private Instant admissionDateTime;
    private String admitSource;
    private String currentBed;
    private String currentRoomCode;
    private String currentWardCode;
    private Instant dischargeDateTime;
    private String ethnicGroup;
    private String fullLocationString;
    private String hospitalService;
    private String mrn;
    private String mergedPatientId;
    private String nhsNumber;
    private Instant patientBirthDate;
    private String patientClass;
    private String patientDeathDateTime;
    private String patientDeathIndicator;
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
    public String getPatientDeathDateTime() {
        return patientDeathDateTime;
    }

    /**
     * @param patientDeathDateTime the patientDeathDateTime to set
     */
    public void setPatientDeathDateTime(String patientDeathDateTime) {
        this.patientDeathDateTime = patientDeathDateTime;
    }

    /**
     * @return the patientDeathIndicator
     */
    public String getPatientDeathIndicator() {
        return patientDeathIndicator;
    }

    /**
     * @param patientDeathIndicator the patientDeathIndicator to set
     */
    public void setPatientDeathIndicator(String patientDeathIndicator) {
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
    public void processMessage(EmapOperationMessageProcessor processor) {
        processor.processMessage(this);
    }
}
