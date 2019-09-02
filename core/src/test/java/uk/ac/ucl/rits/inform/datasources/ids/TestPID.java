package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.datasources.ids.AdtWrap;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.PIDWrap;


/**
 * Test the PID wrapper.
 */
@ActiveProfiles("test")
public class TestPID extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private PIDWrap wrapper;

    @Override
    public void setUp() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");
        wrapper = new AdtWrap(HL7Utils.parseHl7String(hl7));
    }

    /**
     * PID-3.1[1] - in Carecast this is the MRN. Will Epic follow this convention?
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientMrn() throws HL7Exception {
        String result = wrapper.getMrn();
        assertEquals("50032556", result);
    }

    /**
     *  PID-3.1[2] - in Carecast this is the NHS number. Will Epic follow this convention?
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientNHSNumber() throws HL7Exception {
        String result = wrapper.getNHSNumber();
        assertEquals("this is a test NHS number", result);
    }

    /**
     * PID-5.1.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientFamilyName1() throws HL7Exception {
        String result = wrapper.getPatientFamilyName();
        assertEquals("INTERFACES", result);
    }

    /**
     * PID-5.2.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientGivenName1() throws HL7Exception {
        String result = wrapper.getPatientGivenName();
        assertEquals("Amendadmission", result);
    }

    /**
     * PID-5.3 Middle name or initial.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientMiddleName1() throws HL7Exception {
        String result = wrapper.getPatientMiddleName();
        assertEquals("Longforenamesecondfn", result);
    }

    /**
     * PID-5.5 Title.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientTitle1() throws HL7Exception {
        String result = wrapper.getPatientTitle();
        assertEquals("LADY", result);
    }

    /**
     * Test full name.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientFullName1() throws HL7Exception {
        String result = wrapper.getPatientFullName();
        assertEquals("LADY Amendadmission Longforenamesecondfn INTERFACES", result);
    }
}
