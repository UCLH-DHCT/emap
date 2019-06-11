package uk.ac.ucl.rits.inform;

import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import ca.uhn.hl7v2.validation.ValidationContext;

import java.time.Instant;

import org.junit.Test;

import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.hl7.EVNWrap;
import uk.ac.ucl.rits.inform.hl7.HL7Utils;

/**
 * Test the EVN wrapper.
 */
public class TestEVN extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private EVNWrap wrapper;

    @Override
    public void setUp() throws Exception {
        System.out.println("**Setting it up in TestEVN!");

        // An example message, adapted from one provided by Atos
        // NB This is a generic string for testing. Atos A01 example messages, for instance, do not have field MSH-5 populated.
        // And it wouldn't have a timestamp in EVN-6.
        // So there will likely be content in fields below which, for a given message type, would actually be blank.
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");

        context = new DefaultHapiContext();
        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);

        // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
        context.setModelClassFactory(mcf);
        parser = context.getPipeParser();

        try {
            wrapper = new AdtWrap(parser.parse(hl7));
        }
        catch (HL7Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Test EVN-1.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetEventType1() throws HL7Exception {
        String result = wrapper.getEventType();
        assertEquals("A01", result);
    }

    /**
     * Test EVN-2.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetRecordedDateTime1() throws HL7Exception {
        Instant result = wrapper.getRecordedDateTime();
        assertEquals(Instant.parse("2012-09-21T17:43:00.00Z"), result);
    }

    /**
     * Test EVN-4.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetEventReasonCode1() throws HL7Exception {
        String result = wrapper.getEventReasonCode();
        assertEquals("ADM", result);
    }

    /**
     * Test EVN-5.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetOperatorID1() throws HL7Exception {
        String result = wrapper.getOperatorID();
        assertEquals("U439966", result);
    }

    /**
     * Test EVN-6 EventOccurred - for Epic only, A02 messages.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testGetEventOccurred1() throws HL7Exception {
        Instant result = wrapper.getEventOccurred();
        assertEquals(Instant.parse("2012-01-01T12:00:00.00Z"), result);
    }

    @Override
    protected void tearDown() throws Exception {
        // System.out.println("Running: tearDown");
    }

}

/* original message from Atos

// An example Atos-provided message
        String hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|||201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";

*/
