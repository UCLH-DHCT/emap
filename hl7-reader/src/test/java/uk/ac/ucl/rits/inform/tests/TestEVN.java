package uk.ac.ucl.rits.inform.tests;

import java.time.Instant;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.HL7Exception;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.pipeline.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.pipeline.hl7.EVNWrap;
import uk.ac.ucl.rits.inform.pipeline.hl7.HL7Utils;

/**
 * Test the EVN wrapper.
 */
@ActiveProfiles("test")
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
