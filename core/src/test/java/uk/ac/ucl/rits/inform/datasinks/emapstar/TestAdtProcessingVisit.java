package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditHospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestAdtProcessingVisit extends MessageProcessingBase {
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    AuditHospitalVisitRepository auditHospitalVisitRepository;

    private List<HospitalVisit> getAllHospitalVisits() {
        return StreamSupport.stream(hospitalVisitRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private List<AuditHospitalVisit> getAllAuditHospitalVisits() {
        return StreamSupport.stream(auditHospitalVisitRepository.findAll().spliterator(), false).collect(Collectors.toList());
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
        List<AuditHospitalVisit> audits = getAllAuditHospitalVisits();
        assertEquals(1, audits.size());
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

    /**
     * Database has newer information than the message, and the message source is trusted
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testOlderMessageDoesntUpdate() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setRecordedDateTime(Instant.parse("2000-01-01T01:01:01Z"));
        dbOps.processMessage(msg);
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        // admission time should not be updated
        assertNull(visit.getAdmissionTime());
    }

    /**
     * Database has information that is not from a trusted source, older message should still be processed
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testOlderAdtMessageUpdatesMinimalCase() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        String encounter = "0999999999";
        msg.setVisitNumber(encounter);
        msg.setRecordedDateTime(Instant.parse("2000-01-01T01:01:01Z"));

        dbOps.processMessage(msg);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(encounter).orElseThrow(NullPointerException::new);
        assertEquals(PatientClass.INPATIENT.toString(), visit.getPatientClass());
        assertEquals("Ambulance", visit.getArrivalMethod());
        assertNotNull(visit.getAdmissionTime());
        assertEquals("EPIC", visit.getSourceSystem());
    }


    /**
     * Duplicate admit message should not create another audit table row
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
        List<AuditHospitalVisit> audits = getAllAuditHospitalVisits();
        assertEquals(1, audits.size());
    }

}
