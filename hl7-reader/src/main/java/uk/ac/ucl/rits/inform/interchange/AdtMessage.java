package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.ac.ucl.rits.inform.datasources.ids.AdtOperationType;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class AdtMessage implements EmapOperationMessage, Serializable {
    private static final long serialVersionUID = -3352058605994102510L;
    public AdtMessage() {
    }
    
    private AdtOperationType operationType;
    Instant recordedDateTime;
    String eventReasonCode;
    Instant eventOccurredDateTime;
    
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
    
    public AdtOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(AdtOperationType operationType) {
        this.operationType = operationType;
    }

    public Instant getRecordedDateTime() {
        return recordedDateTime;
    }

    public void setRecordedDateTime(Instant recordedDateTime) {
        this.recordedDateTime = recordedDateTime;
    }

    public String getEventReasonCode() {
        return eventReasonCode;
    }

    public void setEventReasonCode(String eventReasonCode) {
        this.eventReasonCode = eventReasonCode;
    }

    public Instant getEventOccurredDateTime() {
        return eventOccurredDateTime;
    }

    public void setEventOccurredDateTime(Instant eventOccurredDateTime) {
        this.eventOccurredDateTime = eventOccurredDateTime;
    }

    public Instant getAdmissionDateTime() {
        return admissionDateTime;
    }

    public void setAdmissionDateTime(Instant admissionDateTime) {
        this.admissionDateTime = admissionDateTime;
    }

    public String getAdmitSource() {
        return admitSource;
    }

    public void setAdmitSource(String admitSource) {
        this.admitSource = admitSource;
    }

    public String getCurrentBed() {
        return currentBed;
    }

    public void setCurrentBed(String currentBed) {
        this.currentBed = currentBed;
    }

    public String getCurrentRoomCode() {
        return currentRoomCode;
    }

    public void setCurrentRoomCode(String currentRoomCode) {
        this.currentRoomCode = currentRoomCode;
    }

    public String getCurrentWardCode() {
        return currentWardCode;
    }

    public void setCurrentWardCode(String currentWardCode) {
        this.currentWardCode = currentWardCode;
    }

    public Instant getDischargeDateTime() {
        return dischargeDateTime;
    }

    public void setDischargeDateTime(Instant dischargeDateTime) {
        this.dischargeDateTime = dischargeDateTime;
    }

    public String getEthnicGroup() {
        return ethnicGroup;
    }

    public void setEthnicGroup(String ethnicGroup) {
        this.ethnicGroup = ethnicGroup;
    }

    public String getFullLocationString() {
        return fullLocationString;
    }

    public void setFullLocationString(String fullLocationString) {
        this.fullLocationString = fullLocationString;
    }

    public String getHospitalService() {
        return hospitalService;
    }

    public void setHospitalService(String hospitalService) {
        this.hospitalService = hospitalService;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getNhsNumber() {
        return nhsNumber;
    }

    public void setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    public Instant getPatientBirthDate() {
        return patientBirthDate;
    }

    public void setPatientBirthDate(Instant patientBirthDate) {
        this.patientBirthDate = patientBirthDate;
    }

    public String getPatientClass() {
        return patientClass;
    }

    public void setPatientClass(String patientClass) {
        this.patientClass = patientClass;
    }

    public String getPatientDeathDateTime() {
        return patientDeathDateTime;
    }

    public void setPatientDeathDateTime(String patientDeathDateTime) {
        this.patientDeathDateTime = patientDeathDateTime;
    }

    public String getPatientDeathIndicator() {
        return patientDeathIndicator;
    }

    public void setPatientDeathIndicator(String patientDeathIndicator) {
        this.patientDeathIndicator = patientDeathIndicator;
    }

    public String getPatientFamilyName() {
        return patientFamilyName;
    }

    public void setPatientFamilyName(String patientFamilyName) {
        this.patientFamilyName = patientFamilyName;
    }

    public String getPatientFullName() {
        return patientFullName;
    }

    public void setPatientFullName(String patientFullName) {
        this.patientFullName = patientFullName;
    }

    public String getPatientGivenName() {
        return patientGivenName;
    }

    public void setPatientGivenName(String patientGivenName) {
        this.patientGivenName = patientGivenName;
    }

    public String getPatientMiddleName() {
        return patientMiddleName;
    }

    public void setPatientMiddleName(String patientMiddleName) {
        this.patientMiddleName = patientMiddleName;
    }

    public String getPatientReligion() {
        return patientReligion;
    }

    public void setPatientReligion(String patientReligion) {
        this.patientReligion = patientReligion;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(String patientTitle) {
        this.patientTitle = patientTitle;
    }

    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    public String getPatientZipOrPostalCode() {
        return patientZipOrPostalCode;
    }

    public void setPatientZipOrPostalCode(String patientZipOrPostalCode) {
        this.patientZipOrPostalCode = patientZipOrPostalCode;
    }

    public String getVisitNumber() {
        return visitNumber;
    }

    public void setVisitNumber(String visitNumber) {
        this.visitNumber = visitNumber;
    }

    public String getMergedPatientId() {
        return mergedPatientId;
    }

    public void setMergedPatientId(String mergedPatientId) {
        this.mergedPatientId = mergedPatientId;
    }

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) {
        processor.processMessage(this);
    }
}
