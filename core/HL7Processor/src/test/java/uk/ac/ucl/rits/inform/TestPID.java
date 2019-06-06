package uk.ac.ucl.rits.inform;

import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;

import org.junit.Test;

import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.hl7.PIDWrap;


/**
 * Test the PID wrapper.
 */
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestPID extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private PID pid;
    private PIDWrap wrapper;

    @Override
    public void setUp() throws Exception {
        System.out.println("**Setting it up in TestMSH!");

        // An example message, adapted from one provided by Atos
        // NB This is a generic string for testing. Atos A01 example messages, for instance, do not have field MSH-5 populated.
        // So there will likely be content in fields below which, for a given message type, would actually be blank.
        String generic_hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|UCLH|ZZ|201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";

        context = new DefaultHapiContext();
        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);

        // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
        context.setModelClassFactory(mcf);
        parser = context.getPipeParser(); //getGenericParser();

        ADT_A01 adtA01 = (ADT_A01) parser.parse(generic_hl7);
        pid = adtA01.getPID();
        wrapper = new PIDWrap(pid);
    }

    /**
     * PID-3.1[1] - in Carecast this is the MRN. Will Epic follow this convention?
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientFirstIdentifier1() throws HL7Exception {
        String result = wrapper.getPatientFirstIdentifier();
        assertEquals("50032556", result);
    }

    /**
     *  PID-3.1[2] - in Carecast this is the NHS number. Will Epic follow this convention?
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetPatientSecondIdentifier1() throws HL7Exception {
        String result = wrapper.getPatientSecondIdentifier();
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

/* original message from Atos

// An example Atos-provided message
        String hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|||201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";

*/
