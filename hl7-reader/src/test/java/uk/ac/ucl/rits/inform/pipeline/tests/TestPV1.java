package uk.ac.ucl.rits.inform.pipeline.tests;

import java.time.Instant;
import java.util.Vector;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.pipeline.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.pipeline.hl7.Doctor;
import uk.ac.ucl.rits.inform.pipeline.hl7.HL7Utils;

/**
 * Test the PV1 wrapper.
 */
@ActiveProfiles("test")
public class TestPV1 extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private AdtWrap wrapper;

    @Override
    public void setUp() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");
        wrapper = new AdtWrap(HL7Utils.parseHl7String(hl7));
    }

    /**
     * Test PV1-3.1.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetCurrentWardCode1() throws HL7Exception {
        String result = wrapper.getCurrentWardCode();
        assertEquals("H2HH", result);
    }

    /**
     * Test PV1-3.2.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetCurrentRoomCode1() throws HL7Exception {
        String result = wrapper.getCurrentRoomCode();
        assertEquals("H203", result);
    }

    /**
     * PV1-3.3.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetCurrentBed1() throws HL7Exception {
        String result = wrapper.getCurrentBed();
        assertEquals("H203-11", result);
    }

    /**
     * PV1-8.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetReferringDoctors1() throws HL7Exception {
        Doctor dr1 = null, dr2 = null;
        Vector<Doctor> vec = wrapper.getReferringDoctors();
        dr1 = vec.get(0);
        dr2 = vec.get(1);
        assertEquals("Wrong dr1 consultant code", "397982", dr1.getConsultantCode());
        assertEquals("Wrong dr2 consultant code", "392275", dr2.getConsultantCode());
        assertEquals("Wrong dr1 surname", "LAWSON", dr1.getSurname());
        assertEquals("Wrong dr2 surname", "ISENBERG", dr2.getSurname());
        assertEquals("Wrong dr1 firstname", "M", dr1.getFirstname());
        assertEquals("Wrong dr2 firstname", "DAVID", dr2.getFirstname());
        assertEquals("Wrong dr1 middlename", null, dr1.getMiddlenameOrInitial());
        assertEquals("Wrong dr2 middlename", "J", dr2.getMiddlenameOrInitial());
        assertEquals("Wrong dr1 title", "DR", dr1.getTitle());
        assertEquals("Wrong dr2 title", "PROF", dr2.getTitle());
        // assertEquals("")getLocalCode()
    }

    /**
     * PV1-10.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetHospitalService1() throws HL7Exception {
        String result = wrapper.getHospitalService();
        assertEquals("41008", result);
    }

    /**
     * PV1-14.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetAdmitSource1() throws HL7Exception {
        String result = wrapper.getAdmitSource();
        assertEquals("19", result);
    }

    /**
     * PV1-18.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetPatientType1() throws HL7Exception {
        String result = wrapper.getPatientType();
        assertEquals(null, result);
    }

    /**
     * PV1-19.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetVisitNumber1() throws HL7Exception {
        String result = wrapper.getVisitNumber();
        assertEquals("1234TESTVISITNUM", result);
    }

    /**
     * PV1-44.1.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetAdmissionDateTime1() throws HL7Exception {
        assertEquals(
                Instant.parse("2012-09-21T17:40:00.00Z"),
                wrapper.getAdmissionDateTime());
    }

    /**
     * PV1-45.1.
     * @throws HL7Exception when HAPI does
     */
    @Test
    public void testGetDischargeDateTime1() throws HL7Exception {
        assertEquals(
                null,
                wrapper.getDischargeDateTime());
    }
}
