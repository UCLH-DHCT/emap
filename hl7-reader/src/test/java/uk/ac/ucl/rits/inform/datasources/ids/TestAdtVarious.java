package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Take an HL7 ADT message as input, and check the correctness of the resultant
 * interchange message (AdtMessage).
 */
public class TestAdtVarious extends TestHl7MessageStream {
    private AdtMessage msg;

    @BeforeEach
    public void setup() throws Exception {
        msg = processSingleAdtMessage("TestForJunit.txt");
    }

    /**
     * Test EVN-2.
     */
    @Test
    public void testGetRecordedDateTime1()  {
        Instant result = msg.getRecordedDateTime();
        assertEquals(Instant.parse("2012-09-21T17:43:00.00Z"), result);
    }

    /**
     * Test EVN-4.
     */
    @Test
    public void testGetEventReasonCode1()  {
        String result = msg.getEventReasonCode();
        assertEquals("ADM", result);
    }

    /**
     * Test EVN-5.
     */
    @Test
    public void testGetOperatorID1()  {
        String result = msg.getOperatorId();
        assertEquals("U439966", result);
    }

    /**
     * Test EVN-6 EventOccurred - for Epic only, A02 messages.
     */
    @Test
    public void testGetEventOccurred1()  {
        Instant result = msg.getEventOccurredDateTime();
        assertEquals(Instant.parse("2012-01-01T12:00:00.00Z"), result);
    }

    /**
     * Test event code
     */
    @Test
    public void testGetOperationType() {
        AdtOperationType result = msg.getOperationType();
        assertEquals(AdtOperationType.ADMIT_PATIENT, result);
    }

    /**
     * PID-3.1[1] - in Carecast this is the MRN. Will Epic follow this convention?
     */
    @Test
    public void testGetPatientMrn()  {
        String result = msg.getMrn();
        assertEquals("50032556", result);
    }

    /**
     *  PID-3.1[2] - in Carecast this is the NHS number. Will Epic follow this convention?
     */
    @Test
    public void testGetPatientNHSNumber()  {
        String result = msg.getNhsNumber();
        assertEquals("this is a test NHS number", result);
    }

    /**
     * PID-5.1.
     */
    @Test
    public void testGetPatientFamilyName1()  {
        String result = msg.getPatientFamilyName();
        assertEquals("INTERFACES", result);
    }

    /**
     * PID-5.2.
     */
    @Test
    public void testGetPatientGivenName1()  {
        String result = msg.getPatientGivenName();
        assertEquals("Amendadmission", result);
    }

    /**
     * PID-5.3 Middle name or initial.
     */
    @Test
    public void testGetPatientMiddleName1()  {
        String result = msg.getPatientMiddleName();
        assertEquals("Longforenamesecondfn", result);
    }

    /**
     * PID-5.5 Title.
     */
    @Test
    public void testGetPatientTitle1()  {
        String result = msg.getPatientTitle();
        assertEquals("LADY", result);
    }

    /**
     * Test full name.
     */
    @Test
    public void testGetPatientFullName1()  {
        String result = msg.getPatientFullName();
        assertEquals("LADY Amendadmission Longforenamesecondfn INTERFACES", result);
    }

    /**
     * Test PV1-3.1.
     */
    @Test
    public void testGetCurrentWardCode1() {
        String result = msg.getCurrentWardCode();
        assertEquals("H2HH", result);
    }

    /**
     * Test PV1-3.2.
     */
    @Test
    public void testGetCurrentRoomCode1() {
        String result = msg.getCurrentRoomCode();
        assertEquals("H203", result);
    }

    /**
     * PV1-3.3.
     */
    @Test
    public void testGetCurrentBed1() {
        String result = msg.getCurrentBed();
        assertEquals("H203-11", result);
    }

    /**
     * PV1-10.
     */
    @Test
    public void testGetHospitalService1() {
        String result = msg.getHospitalService();
        assertEquals("41008", result);
    }

    /**
     * PV1-14.
     */
    @Test
    public void testGetAdmitSource1() {
        String result = msg.getAdmitSource();
        assertEquals("19", result);
    }

    /**
     * PV1-18.
     */
    @Test
    public void testGetPatientType1() {
        String result = msg.getPatientType();
        assertEquals(null, result);
    }

    /**
     * PV1-19.
     */
    @Test
    public void testGetVisitNumber1() {
        String result = msg.getVisitNumber();
        assertEquals("1234TESTVISITNUM", result);
    }

    @Test
    public void testGetDischargeLocation() {
        assertEquals("Home", msg.getDischargeLocation());
    }

    /**
     * PV1-44.1.
     */
    @Test
    public void testGetAdmissionDateTime1() {
        assertEquals(
                Instant.parse("2012-09-21T17:40:00.00Z"),
                msg.getAdmissionDateTime());
    }

    /**
     * PV1-45.1.
     */
    @Test
    public void testGetDischargeDateTime1() {
        assertEquals(
                null,
                msg.getDischargeDateTime());
    }

    @Test
    public void testNoTimeOfDeath()  {
        assertNull(msg.getPatientDeathDateTime());
    }

    @Test
    /**
     * Not an A03, death indicator shouldn't be set
     */
    public void testIsNull()  {
        assertNull(msg.getPatientDeathIndicator());
    }
}
