package uk.ac.ucl.rits.inform.interchange.adt;

public interface AdtMessageInterface {
    java.time.Instant getRecordedDateTime();

    String getEventReasonCode();

    java.time.Instant getEventOccurredDateTime();

    String getOperatorId();

    java.time.Instant getAdmissionDateTime();

    String getAdmitSource();

    String getCurrentBed();

    String getCurrentRoomCode();

    String getCurrentWardCode();

    String getEthnicGroup();

    String getFullLocationString();

    String getHospitalService();

    String getMrn();

    String getNhsNumber();

    java.time.Instant getPatientBirthDate();

    String getPatientClass();

    java.time.Instant getPatientDeathDateTime();

    Boolean getPatientDeathIndicator();

    String getPatientFamilyName();

    String getPatientFullName();

    String getPatientGivenName();

    String getPatientMiddleName();

    String getPatientReligion();

    String getPatientSex();

    String getPatientTitle();

    String getPatientType();

    String getPatientZipOrPostalCode();

    String getVisitNumber();

    void setRecordedDateTime(java.time.Instant recordedDateTime);

    void setEventReasonCode(String eventReasonCode);

    void setEventOccurredDateTime(java.time.Instant eventOccurredDateTime);

    void setOperatorId(String operatorId);

    void setAdmissionDateTime(java.time.Instant admissionDateTime);

    void setAdmitSource(String admitSource);

    void setCurrentBed(String currentBed);

    void setCurrentRoomCode(String currentRoomCode);

    void setCurrentWardCode(String currentWardCode);

    void setEthnicGroup(String ethnicGroup);

    void setFullLocationString(String fullLocationString);

    void setHospitalService(String hospitalService);

    void setMrn(String mrn);

    void setNhsNumber(String nhsNumber);

    void setPatientBirthDate(java.time.Instant patientBirthDate);

    void setPatientClass(String patientClass);

    void setPatientDeathDateTime(java.time.Instant patientDeathDateTime);

    void setPatientDeathIndicator(Boolean patientDeathIndicator);

    void setPatientFamilyName(String patientFamilyName);

    void setPatientFullName(String patientFullName);

    void setPatientGivenName(String patientGivenName);

    void setPatientMiddleName(String patientMiddleName);

    void setPatientReligion(String patientReligion);

    void setPatientSex(String patientSex);

    void setPatientTitle(String patientTitle);

    void setPatientType(String patientType);

    void setPatientZipOrPostalCode(String patientZipOrPostalCode);

    void setVisitNumber(String visitNumber);
}
