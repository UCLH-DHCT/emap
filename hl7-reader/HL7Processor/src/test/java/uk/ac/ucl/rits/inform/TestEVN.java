package uk.ac.ucl.rits.inform;

import java.time.Instant;

import org.junit.Test;

import ca.uhn.hl7v2.HL7Exception;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.hl7.EVNWrap;
import uk.ac.ucl.rits.inform.hl7.HL7Utils;

/**
 * Test the EVN wrapper.
 */
public class TestEVN extends TestCase {
    private EVNWrap wrapper;

    @Override
    public void setUp() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");
        wrapper = new AdtWrap(HL7Utils.parseHl7String(hl7));
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
