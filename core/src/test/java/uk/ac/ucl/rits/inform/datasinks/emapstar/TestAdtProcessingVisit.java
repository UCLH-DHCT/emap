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

    String defaultEncounter = "123412341234";

    private List<HospitalVisit> getAllHospitalVisits() {
        return StreamSupport.stream(hospitalVisitRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private List<AuditHospitalVisit> getAllAuditHospitalVisits() {
        return StreamSupport.stream(auditHospitalVisitRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    /**
     * No existing hospital visits, so should make a new visit. Admission date time should be set, but presentation time should not.
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
     * hospital visit already exists for encounter, with a presentation time, but no admission time
     * Admission time should be added, but presentation time should not be added. Stored/valid from should be updated
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

}
