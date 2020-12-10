package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisitAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelAdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdatePatientInfo;

public class TestAdtProcessingVisit extends MessageProcessingBase {
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    HospitalVisitAuditRepository hospitalVisitAuditRepository;

    private List<HospitalVisit> getAllHospitalVisits() {
        return StreamSupport.stream(hospitalVisitRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private List<HospitalVisitAudit> getAllAuditHospitalVisits() {
        return StreamSupport.stream(hospitalVisitAuditRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    /**
     * No existing hospital visits, so should make a new visit. Admission date time should be set, but presentation time should not.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    public void testCreateNewAdmit() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);

        List<HospitalVisit> visits = getAllHospitalVisits();
        assertEquals(1, visits.size());

        HospitalVisit visit = visits.get(0);
        assertNotNull(visit.getAdmissionTime());
        assertNull(visit.getPresentationTime());
        // no audit log should be added
        assertTrue(getAllAuditHospitalVisits().isEmpty());
    }

    /**
     * No existing hospital visits, so should make a new visit.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    public void testMoveVisitInformationCreatesVisitIfItDoesntExist() throws EmapOperationMessageProcessingException {
        MoveVisitInformation msg = messageFactory.getAdtMessage("generic/A45.yaml");
        dbOps.processMessage(msg);

        List<HospitalVisit> visits = getAllHospitalVisits();
        assertEquals(1, visits.size());

        HospitalVisit visit = visits.get(0);
        // no audit log should be added
        assertTrue(getAllAuditHospitalVisits().isEmpty());
    }

    /**
     * No existing hospital visits, so should make a new visit. Presentation time should be set, admission time should not.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    public void testCreateNewRegistration() throws EmapOperationMessageProcessingException {
        RegisterPatient msg = messageFactory.getAdtMessage("generic/A04.yaml");
        dbOps.processMessage(msg);

        List<HospitalVisit> visits = getAllHospitalVisits();
        assertEquals(1, visits.size());

        HospitalVisit visit = visits.get(0);
        assertNull(visit.getAdmissionTime());
        assertNotNull(visit.getPresentationTime());
    }

    /**
     * hospital visit already exists for encounter, with a presentation time, but no admission time
     * Admission time should be added, but presentation time should not be added. Stored/valid from should be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testAdmitWhenExistingPresentation() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // Presentation time should not be changed by admission message
        assertEquals(Instant.parse("2012-09-17T13:25:00.65Z"), visit.getPresentationTime());
        // Admission time should be changed
        assertEquals(Instant.parse("2013-02-11T10:00:52Z"), visit.getAdmissionTime());
        // validFrom and Stored from should be updated
        Instant originalStoredFrom = Instant.parse("2012-09-17T13:25:00.650Z");
        assertTrue(visit.getStoredFrom().isAfter(originalStoredFrom));
        assertTrue(visit.getValidFrom().isAfter(originalStoredFrom));

        // Auditlog should now have have one row
        List<HospitalVisitAudit> audits = getAllAuditHospitalVisits();
        assertEquals(1, audits.size());
    }

    /**
     * Admit a patient and then cancel the admit.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    public void testAdmitThenCancelAdmit() throws EmapOperationMessageProcessingException {
        AdmitPatient addMsg = messageFactory.getAdtMessage("generic/A01.yaml");
        CancelAdmitPatient removeMsg = messageFactory.getAdtMessage("generic/A11.yaml");

        dbOps.processMessage(addMsg);
        dbOps.processMessage(removeMsg);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        assertNull(visit.getAdmissionTime());
        assertEquals(removeMsg.getCancelledDateTime(), visit.getValidFrom());
        assertEquals(PatientClass.OUTPATIENT.toString(), visit.getPatientClass());
    }

    /**
     * Discharge from visit with minimal information from untrusted source.
     * Admission information should be added, along with discharge.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testDischargePatientWithNoKnownAdmission() throws EmapOperationMessageProcessingException {
        DischargePatient msg = messageFactory.getAdtMessage("generic/A03.yaml");
        String visitNumber = "0999999999";
        msg.setVisitNumber(visitNumber);

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(visitNumber).orElseThrow(NullPointerException::new);
        // generic information should be added
        assertNotNull(visit.getPatientClass());
        // admission information should be added
        assertNotNull(visit.getAdmissionTime());
        // discharge information should be added
        assertNotNull(visit.getDischargeDestination());
        assertNotNull(visit.getDischargeDisposition());
        assertNotNull(visit.getDischargeTime());
    }

    @Test
    @Sql(value = "/populate_db.sql")
    public void testDischargeThenCancelDischarge() throws EmapOperationMessageProcessingException {
        DischargePatient addMsg = messageFactory.getAdtMessage("generic/A03.yaml");
        CancelDischargePatient removeMsg = messageFactory.getAdtMessage("generic/A13.yaml");

        dbOps.processMessage(addMsg);
        dbOps.processMessage(removeMsg);


        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // discharge information should be removed
        assertNull(visit.getDischargeDestination());
        assertNull(visit.getDischargeDisposition());
        assertNull(visit.getDischargeTime());
        // cancelled date time is missing from message, so should use recordedDateTime
        assertEquals(removeMsg.getRecordedDateTime(), visit.getValidFrom());
    }


    /**
     * Database has newer information than the message, but no admission time and the message source is trusted
     * Admit message should only update the admission time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOlderAdmitMessageUpdatesAdmissionTimeOnly() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setRecordedDateTime(past);
        msg.setEventOccurredDateTime(null);

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // admission time should be updated because it is null
        Assertions.assertNotNull(visit.getAdmissionTime());
        // arrival method should not be updated from the message
        Assertions.assertNotEquals(msg.getModeOfArrival(), visit.getArrivalMethod());
    }

    /**
     * Database has newer information than the message (including admission time) and the message source is trusted
     * Admit message should not update the admission time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOlderAdmitMessageDoesntUpdateExistingAdmissionTime() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setRecordedDateTime(past);
        msg.setEventOccurredDateTime(null);
        msg = setDataForHospitalVisitId4002(msg);

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // admission time should not be updated
        Assertions.assertNotEquals(msg.getAdmissionDateTime(), visit.getAdmissionTime());
    }

    /**
     * Database has newer information than the message, but no presentation time and the message source is trusted
     * Register message should only update the presentation time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOlderRegisterMessageUpdatesPresentationTimeOnly() throws EmapOperationMessageProcessingException {
        RegisterPatient msg = messageFactory.getAdtMessage("generic/A04.yaml");
        msg.setRecordedDateTime(past);
        msg.setEventOccurredDateTime(null);

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // admission time should be updated because it is null
        Assertions.assertNotNull(visit.getPresentationTime());
        // arrival method should not be updated from the message
        Assertions.assertNotEquals(msg.getModeOfArrival(), visit.getArrivalMethod());
    }

    /**
     * Database has newer information than the message (including presentation time) and the message source is trusted
     * Register message should not update the presentation time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOlderRegisterMessageDoesntUpdateExistingPresentationTime() throws EmapOperationMessageProcessingException {
        RegisterPatient msg = messageFactory.getAdtMessage("generic/A04.yaml");
        msg.setRecordedDateTime(past);
        msg.setEventOccurredDateTime(null);
        msg = setDataForHospitalVisitId4002(msg);

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // admission time should not be updated
        Assertions.assertNotEquals(msg.getPresentationDateTime(), visit.getPresentationTime());
    }

    /**
     * Current visit if from untrusted source and new message is from an untrusted source,
     * visit should not be updated because we don't trust the update.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testUntrustedSourceMessageDoesNotUpdate() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        String untrustedEncounter = "0999999999";
        msg.setVisitNumber(untrustedEncounter);
        msg.setMrn("30700000");
        msg.setSourceSystem("don't trust me");

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(untrustedEncounter).orElseThrow(NullPointerException::new);
        // admission time should be updated
        assertNull(visit.getAdmissionTime());
    }

    /**
     * database visit is from untrusted source, a message with old visit information from a trusted source should update the visit.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOlderMessageUpdatesIfCurrentVisitIsFromUntrustedSource() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        String untrustedEncounter = "0999999999";
        msg.setVisitNumber(untrustedEncounter);
        msg.setMrn(null);
        msg.setNhsNumber("222222222");
        msg.setRecordedDateTime(past);
        msg.setEventOccurredDateTime(past);

        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(untrustedEncounter).orElseThrow(NullPointerException::new);
        // admission time should be updated
        assertNotNull(visit.getAdmissionTime());
    }

    /**
     * Database has information that is not from a trusted source, older message should still be processed.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testOlderAdtMessageUpdatesMinimalCase() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        String encounter = "0999999999";
        msg.setVisitNumber(encounter);
        msg.setRecordedDateTime(past);

        dbOps.processMessage(msg);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(encounter).orElseThrow(NullPointerException::new);
        assertEquals(PatientClass.INPATIENT.toString(), visit.getPatientClass());
        assertEquals("Ambulance", visit.getArrivalMethod());
        assertNotNull(visit.getAdmissionTime());
        assertEquals("EPIC", visit.getSourceSystem());
    }


    /**
     * Duplicate admit message should not create another audit table row.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testDuplicateAdmitMessage() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);
        dbOps.processMessage(msg);

        // should only be one encounter for this mrn
        List<HospitalVisit> mrnVisits = hospitalVisitRepository.findAllByMrnIdMrnId(1001L).orElseThrow(NullPointerException::new);
        assertEquals(1, mrnVisits.size());
        // Auditlog should now have have one row
        List<HospitalVisitAudit> audits = getAllAuditHospitalVisits();
        assertEquals(1, audits.size());
    }

    /**
     * All visits should be logged to audit table and deleted.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testDeletePersonInformation() throws EmapOperationMessageProcessingException {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        dbOps.processMessage(msg);

        // visit should no longer exist
        Optional<HospitalVisit> visit = hospitalVisitRepository.findByEncounter(defaultEncounter);
        assertFalse(visit.isPresent());

        // Auditlog should now have have one row
        List<HospitalVisitAudit> audits = getAllAuditHospitalVisits();
        assertEquals(1, audits.size());
    }

    /**
     * Older message should to nothing.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testOlderMessageDeleteDoesNothing() throws EmapOperationMessageProcessingException {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        msg.setEventOccurredDateTime(past);
        dbOps.processMessage(msg);

        // visit should no longer exist
        Optional<HospitalVisit> visit = hospitalVisitRepository.findByEncounter(defaultEncounter);
        assertTrue(visit.isPresent());

        // no audit rows
        List<HospitalVisitAudit> audits = getAllAuditHospitalVisits();
        assertEquals(0, audits.size());
    }

    /**
     * MoveVisitInformation for existing MRNs and visits.
     * Should change the MRN and encounter string,
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testMoveVisitInformation() throws EmapOperationMessageProcessingException {
        MoveVisitInformation msg = messageFactory.getAdtMessage("generic/A45.yaml");
        dbOps.processMessage(msg);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // should be changed from default Mrn value
        assertNotEquals(defaultMrn, visit.getMrnId().getMrn());

        // Audit log should exist
        HospitalVisitAudit audit = hospitalVisitAuditRepository.findByEncounter(defaultEncounter);
        assertEquals(defaultMrn, audit.getMrnId().getMrn());
    }

    /**
     * UpdatePatientInfo that doesn't have a visit number should still be parsed, but no visit information should change.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testUpdatePatientInfoWithNoEncounterStillCompletesProcessing() throws EmapOperationMessageProcessingException {
        UpdatePatientInfo msg = messageFactory.getAdtMessage("generic/A08_v1.yaml");
        msg.setVisitNumber(null);

        HospitalVisit preProcessingVisit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);

        dbOps.processMessage(msg);

        // No audit log should exist because visit was not updated
        assertTrue(getAllAuditHospitalVisits().isEmpty());
    }


}
