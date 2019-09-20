package uk.ac.ucl.rits.inform.datasources.ids;

import java.time.Instant;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.model.Message;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Test the EVN wrapper.
 */
@ActiveProfiles("test")
public class TestAdt extends TestCase {
    private AdtMessage wrapper;

    @Override
    public void setUp() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        wrapper = new AdtMessageBuilder(hl7Msg).getAdtMessage();
    }

    /**
     * Test EVN-2.
     */
    @Test
    public void testGetRecordedDateTime1()  {
        Instant result = wrapper.getRecordedDateTime();
        assertEquals(Instant.parse("2012-09-21T17:43:00.00Z"), result);
    }

    /**
     * Test EVN-4.
     */
    @Test
    public void testGetEventReasonCode1()  {
        String result = wrapper.getEventReasonCode();
        assertEquals("ADM", result);
    }

    /**
     * Test EVN-5.
     */
    @Test
    public void testGetOperatorID1()  {
        String result = wrapper.getOperatorId();
        assertEquals("U439966", result);
    }

    /**
     * Test EVN-6 EventOccurred - for Epic only, A02 messages.
     */
    @Test
    public void testGetEventOccurred1()  {
        Instant result = wrapper.getEventOccurredDateTime();
        assertEquals(Instant.parse("2012-01-01T12:00:00.00Z"), result);
    }


    /**
     * Test event code
     */
    @Test
    public void testGetOperationType() {
        AdtOperationType result = wrapper.getOperationType();
        assertEquals(AdtOperationType.ADMIT_PATIENT, result);
    }

    /**
     * PID-3.1[1] - in Carecast this is the MRN. Will Epic follow this convention?
     */
    @Test
    public void testGetPatientMrn()  {
        String result = wrapper.getMrn();
        assertEquals("50032556", result);
    }

    /**
     *  PID-3.1[2] - in Carecast this is the NHS number. Will Epic follow this convention?
     */
    @Test
    public void testGetPatientNHSNumber()  {
        String result = wrapper.getNhsNumber();
        assertEquals("this is a test NHS number", result);
    }

    /**
     * PID-5.1.
     */
    @Test
    public void testGetPatientFamilyName1()  {
        String result = wrapper.getPatientFamilyName();
        assertEquals("INTERFACES", result);
    }

    /**
     * PID-5.2.
     */
    @Test
    public void testGetPatientGivenName1()  {
        String result = wrapper.getPatientGivenName();
        assertEquals("Amendadmission", result);
    }

    /**
     * PID-5.3 Middle name or initial.
     */
    @Test
    public void testGetPatientMiddleName1()  {
        String result = wrapper.getPatientMiddleName();
        assertEquals("Longforenamesecondfn", result);
    }

    /**
     * PID-5.5 Title.
     */
    @Test
    public void testGetPatientTitle1()  {
        String result = wrapper.getPatientTitle();
        assertEquals("LADY", result);
    }

    /**
     * Test full name.
     */
    @Test
    public void testGetPatientFullName1()  {
        String result = wrapper.getPatientFullName();
        assertEquals("LADY Amendadmission Longforenamesecondfn INTERFACES", result);
    }

    /**
     * Test PV1-3.1.
     */
    @Test
    public void testGetCurrentWardCode1() {
        String result = wrapper.getCurrentWardCode();
        assertEquals("H2HH", result);
    }

    /**
     * Test PV1-3.2.
     */
    @Test
    public void testGetCurrentRoomCode1() {
        String result = wrapper.getCurrentRoomCode();
        assertEquals("H203", result);
    }

    /**
     * PV1-3.3.
     */
    @Test
    public void testGetCurrentBed1() {
        String result = wrapper.getCurrentBed();
        assertEquals("H203-11", result);
    }

    /**
     * PV1-8.
     */
//    @Test
//    public void testGetReferringDoctors1() {
//        Doctor dr1 = null, dr2 = null;
//        Vector<Doctor> vec = wrapper.getReferringDoctors();
//        dr1 = vec.get(0);
//        dr2 = vec.get(1);
//        assertEquals("Wrong dr1 consultant code", "397982", dr1.getConsultantCode());
//        assertEquals("Wrong dr2 consultant code", "392275", dr2.getConsultantCode());
//        assertEquals("Wrong dr1 surname", "LAWSON", dr1.getSurname());
//        assertEquals("Wrong dr2 surname", "ISENBERG", dr2.getSurname());
//        assertEquals("Wrong dr1 firstname", "M", dr1.getFirstname());
//        assertEquals("Wrong dr2 firstname", "DAVID", dr2.getFirstname());
//        assertEquals("Wrong dr1 middlename", null, dr1.getMiddlenameOrInitial());
//        assertEquals("Wrong dr2 middlename", "J", dr2.getMiddlenameOrInitial());
//        assertEquals("Wrong dr1 title", "DR", dr1.getTitle());
//        assertEquals("Wrong dr2 title", "PROF", dr2.getTitle());
//        // assertEquals("")getLocalCode()
//    }

    /**
     * PV1-10.
     */
    @Test
    public void testGetHospitalService1() {
        String result = wrapper.getHospitalService();
        assertEquals("41008", result);
    }

    /**
     * PV1-14.
     */
    @Test
    public void testGetAdmitSource1() {
        String result = wrapper.getAdmitSource();
        assertEquals("19", result);
    }

    /**
     * PV1-18.
     */
    @Test
    public void testGetPatientType1() {
        String result = wrapper.getPatientType();
        assertEquals(null, result);
    }

    /**
     * PV1-19.
     */
    @Test
    public void testGetVisitNumber1() {
        String result = wrapper.getVisitNumber();
        assertEquals("1234TESTVISITNUM", result);
    }

    /**
     * PV1-44.1.
     */
    @Test
    public void testGetAdmissionDateTime1() {
        assertEquals(
                Instant.parse("2012-09-21T17:40:00.00Z"),
                wrapper.getAdmissionDateTime());
    }

    /**
     * PV1-45.1.
     */
    @Test
    public void testGetDischargeDateTime1() {
        assertEquals(
                null,
                wrapper.getDischargeDateTime());
    }
}
