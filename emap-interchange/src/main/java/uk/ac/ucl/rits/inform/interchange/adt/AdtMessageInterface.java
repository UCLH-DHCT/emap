package uk.ac.ucl.rits.inform.interchange.adt;

import uk.ac.ucl.rits.inform.interchange.Hl7Value;

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
    Hl7Value<Instant> getAdmissionDateTime();

    /**
     * @return admitSource
     */
    Hl7Value<String> getAdmitSource();

    /**
     * @return currentBed
     */
    Hl7Value<String> getCurrentBed();

    /**
     * @return currentRoomCode
     */
    Hl7Value<String> getCurrentRoomCode();

    /**
     * @return currentWardCode
     */
    Hl7Value<String> getCurrentWardCode();

    /**
     * @return ethnicGroup
     */
    Hl7Value<String> getEthnicGroup();

    /**
     * @return fullLocationHl7Field<String>
     */
    Hl7Value<String> getFullLocationString();

    /**
     * @return hospitalService
     */
    Hl7Value<String> getHospitalService();

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
    Hl7Value<Instant> getPatientBirthDate();

    /**
     * @return patientClass
     */
    Hl7Value<String> getPatientClass();

    /**
     * @return patientDeathDateTime
     */
    Hl7Value<Instant> getPatientDeathDateTime();

    /**
     * @return patientDeathIndicator
     */
    Hl7Value<Boolean> getPatientDeathIndicator();

    /**
     * @return patientFamilyName
     */
    Hl7Value<String> getPatientFamilyName();

    /**
     * @return patientFullName
     * keep this? seems redundant and not used?
     */
    Hl7Value<String> getPatientFullName();

    /**
     * @return patientGivenName
     */
    Hl7Value<String> getPatientGivenName();

    /**
     * @return patientMiddleName
     */
    Hl7Value<String> getPatientMiddleName();

    /**
     * @return patientReligion
     */
    Hl7Value<String> getPatientReligion();

    /**
     * @return patientSex
     */
    Hl7Value<String> getPatientSex();

    /**
     * @return patientTitle
     */
    Hl7Value<String> getPatientTitle();

    /**
     * @return patientType
     */
    Hl7Value<String> getPatientType();

    /**
     * @return patientZipOrPostalCode
     */
    Hl7Value<String> getPatientZipOrPostalCode();

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
    void setAdmissionDateTime(Hl7Value<Instant> admissionDateTime);

    /**
     * @param admitSource to set
     */
    void setAdmitSource(Hl7Value<String> admitSource);

    /**
     * @param currentBed to set
     */
    void setCurrentBed(Hl7Value<String> currentBed);

    /**
     * @param currentRoomCode to set
     */
    void setCurrentRoomCode(Hl7Value<String> currentRoomCode);

    /**
     * @param currentWardCode to set
     */
    void setCurrentWardCode(Hl7Value<String> currentWardCode);

    /**
     * @param ethnicGroup to set
     */
    void setEthnicGroup(Hl7Value<String> ethnicGroup);

    /**
     * @param fullLocationString to set
     */
    void setFullLocationString(Hl7Value<String> fullLocationString);

    /**
     * @param hospitalService to set
     */
    void setHospitalService(Hl7Value<String> hospitalService);

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
    void setPatientBirthDate(Hl7Value<Instant> patientBirthDate);

    /**
     * @param patientClass to set
     */
    void setPatientClass(Hl7Value<String> patientClass);

    /**
     * @param patientDeathDateTime to set
     */
    void setPatientDeathDateTime(Hl7Value<Instant> patientDeathDateTime);

    /**
     * @param patientDeathIndicator to set
     */
    void setPatientDeathIndicator(Hl7Value<Boolean> patientDeathIndicator);

    /**
     * @param patientFamilyName to set
     */
    void setPatientFamilyName(Hl7Value<String> patientFamilyName);

    /**
     * @param patientFullName to set
     */
    void setPatientFullName(Hl7Value<String> patientFullName);

    /**
     * @param patientGivenName to set
     */
    void setPatientGivenName(Hl7Value<String> patientGivenName);

    /**
     * @param patientMiddleName to set
     */
    void setPatientMiddleName(Hl7Value<String> patientMiddleName);

    /**
     * @param patientReligion to set
     */
    void setPatientReligion(Hl7Value<String> patientReligion);

    /**
     * @param patientSex to set
     */
    void setPatientSex(Hl7Value<String> patientSex);

    /**
     * @param patientTitle to set
     */
    void setPatientTitle(Hl7Value<String> patientTitle);

    /**
     * @param patientType to set
     */
    void setPatientType(Hl7Value<String> patientType);

    /**
     * @param patientZipOrPostalCode to set
     */
    void setPatientZipOrPostalCode(Hl7Value<String> patientZipOrPostalCode);

    /**
     * @param visitNumber to set
     */
    void setVisitNumber(String visitNumber);
}
