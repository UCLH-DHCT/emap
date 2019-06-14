package uk.ac.ucl.rits.inform;

import org.junit.Test;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.hl7.HL7Utils;
import uk.ac.ucl.rits.inform.hl7.MSHWrap;

/**
 * Test MSH wrapper.
 */
public class TestMSH extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private MSHWrap wrapper;

    @Override
    public void setUp() throws Exception {
        System.out.println("**Setting it up in TestMSH!");

        // An example message, adapted from one provided by Atos
        // NB This is a generic string for testing. Atos A01 example messages, for instance, do not have field MSH-5 populated.
        // So there will likely be content in fields below which, for a given message type, would actually be blank.
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");
        context = new DefaultHapiContext();
        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);

        // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
        context.setModelClassFactory(mcf);
        parser = context.getPipeParser();

        ADT_A01 adtA01 = (ADT_A01) parser.parse(hl7);
        wrapper = new AdtWrap(adtA01);
    }

    /**
     * Test MSH-3.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetSendingApplication1() throws HL7Exception {
        String result = wrapper.getSendingApplication();
        assertEquals("UCLH4", result);
    }

    /**
     * Test MSH-4.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetSendingFacility1() throws HL7Exception {
        String result = wrapper.getSendingFacility();
        assertEquals("RRV30", result);
    }

    /**
     * Test MSH-5.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetReceivingApplication1() throws HL7Exception {
        String result = wrapper.getReceivingApplication();
        assertEquals("UCLH", result);
    }

    /**
     * Test MSH-6.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetReceivingFacility1() throws HL7Exception {
        String result = wrapper.getReceivingFacility();
        assertEquals("ZZ", result);
    }

    /**
     * Test MSH-7.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetMessageTimestamp1() throws HL7Exception {
        String result = wrapper.getMessageTimestamp();
        assertEquals("201209211843", result);
    }

    /**
     * Test MSH-9.1.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetMessageType1() throws HL7Exception {
        String result = wrapper.getMessageType();
        assertEquals("ADT", result);
    }

    /**
     * Test MSH-9.2.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetTriggerEvent1() throws HL7Exception {
        String result = wrapper.getTriggerEvent();
        assertEquals("A01", result);
    }

    /**
     * Test MSH-10.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetMessageControlID1() throws HL7Exception {
        String result = wrapper.getMessageControlID();
        assertEquals("PLW21221500942883310", result);
    }

    /**
     * Test MSH-11.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetProcessingID1() throws HL7Exception {
        String result = wrapper.getProcessingID();
        assertEquals("P", result);
    }

    /**
     * Test MSH-12.
     * Even though we "converted" the 2.2 message to a 2.7 message, this field still returns 2.2
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetVersionID1() throws HL7Exception {
        String result = wrapper.getVersionID();
        assertEquals("2.2", result);
    }
}

/* original message from Atos

// An example Atos-provided message
        String hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|||201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";



*/
