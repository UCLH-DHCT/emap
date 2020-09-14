package uk.ac.ucl.rits.inform.interchange.adt;

import java.time.Instant;

/**
 * Interface for data that is in all Adt Messages.
 */
public interface AdtMessageInterface {
    /**
     * @return recordedDateTime
     */
    Instant getRecordedDateTime();

    /**
     * @return eventReasonCode
     */
    String getEventReasonCode();

    /**
     * @return eventOccuredDatetime.
     */
    Instant getEventOccurredDateTime();

    /**
     * @return operatorId
     */
    String getOperatorId();

    /**
     * @return admissionDateTime
     */
    Instant getAdmissionDateTime();

    /**
     * @return admitSource
     */
    String getAdmitSource();

    /**
     * @return currentBed
     */
    String getCurrentBed();

    /**
     * @return currentRoomCode
     */
    String getCurrentRoomCode();

    /**
     * @return currentWardCode
     */
    String getCurrentWardCode();

    /**
     * @return ethnicGroup
     */
    String getEthnicGroup();

    /**
     * @return fullLocationString
     */
    String getFullLocationString();

    /**
     * @return hospitalService
     */
    String getHospitalService();

    /**
     * @return mrn
     */
    String getMrn();

    /**
     * @return nhsNumber
     */
    String getNhsNumber();

    /**
     * @return patientBirthDate
     */
    Instant getPatientBirthDate();

    /**
     * @return patientClass
     */
    String getPatientClass();

    /**
     * @return patientDeathDateTime
     */
    Instant getPatientDeathDateTime();

    /**
     * @return patientDeathIndicator
     */
    Boolean getPatientDeathIndicator();

    /**
     * @return patientFamilyName
     */
    String getPatientFamilyName();

    /**
     * @return patientFullName
     * keep this? seems redundant and not used?
     */
    String getPatientFullName();

    /**
     * @return patientGivenName
     */
    String getPatientGivenName();

    /**
     * @return patientMiddleName
     */
    String getPatientMiddleName();

    /**
     * @return patientReligion
     */
    String getPatientReligion();

    /**
     * @return patientSex
     */
    String getPatientSex();

    /**
     * @return patientTitle
     */
    String getPatientTitle();

    /**
     * @return patientType
     */
    String getPatientType();

    /**
     * @return patientZipOrPostalCode
     */
    String getPatientZipOrPostalCode();

    /**
     * @return visitNumber
     */
    String getVisitNumber();

    /**
     * @param recordedDateTime to set
     */
    void setRecordedDateTime(Instant recordedDateTime);

    /**
     * @param eventReasonCode to set
     */
    void setEventReasonCode(String eventReasonCode);

    /**
     * @param eventOccurredDateTime to set
     */
    void setEventOccurredDateTime(Instant eventOccurredDateTime);

    /**
     * @param operatorId to set
     */
    void setOperatorId(String operatorId);

    /**
     * @param admissionDateTime to set
     */
    void setAdmissionDateTime(Instant admissionDateTime);

    /**
     * @param admitSource to set
     */
    void setAdmitSource(String admitSource);

    /**
     * @param currentBed to set
     */
    void setCurrentBed(String currentBed);

    /**
     * @param currentRoomCode to set
     */
    void setCurrentRoomCode(String currentRoomCode);

    /**
     * @param currentWardCode to set
     */
    void setCurrentWardCode(String currentWardCode);

    /**
     * @param ethnicGroup to set
     */
    void setEthnicGroup(String ethnicGroup);

    /**
     * @param fullLocationString to set
     */
    void setFullLocationString(String fullLocationString);

    /**
     * @param hospitalService to set
     */
    void setHospitalService(String hospitalService);

    /**
     * @param mrn to set
     */
    void setMrn(String mrn);

    /**
     * @param nhsNumber to set
     */
    void setNhsNumber(String nhsNumber);

    /**
     * @param patientBirthDate to set
     */
    void setPatientBirthDate(Instant patientBirthDate);

    /**
     * @param patientClass to set
     */
    void setPatientClass(String patientClass);

    /**
     * @param patientDeathDateTime to set
     */
    void setPatientDeathDateTime(Instant patientDeathDateTime);

    /**
     * @param patientDeathIndicator to set
     */
    void setPatientDeathIndicator(Boolean patientDeathIndicator);

    /**
     * @param patientFamilyName to set
     */
    void setPatientFamilyName(String patientFamilyName);

    /**
     * @param patientFullName to set
     */
    void setPatientFullName(String patientFullName);

    /**
     * @param patientGivenName to set
     */
    void setPatientGivenName(String patientGivenName);

    /**
     * @param patientMiddleName to set
     */
    void setPatientMiddleName(String patientMiddleName);

    /**
     * @param patientReligion to set
     */
    void setPatientReligion(String patientReligion);

    /**
     * @param patientSex to set
     */
    void setPatientSex(String patientSex);

    /**
     * @param patientTitle to set
     */
    void setPatientTitle(String patientTitle);

    /**
     * @param patientType to set
     */
    void setPatientType(String patientType);

    /**
     * @param patientZipOrPostalCode to set
     */
    void setPatientZipOrPostalCode(String patientZipOrPostalCode);

    /**
     * @param visitNumber to set
     */
    void setVisitNumber(String visitNumber);
}
