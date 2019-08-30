package uk.ac.ucl.rits.inform.tests;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.datasources.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.datasources.hl7.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.hl7.MSHWrap;

/**
 * Test MSH wrapper.
 */
@ActiveProfiles("test")
public class TestMSH extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private MSHWrap wrapper;

    @Override
    public void setUp() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("TestForJunit.txt");
        wrapper = new AdtWrap(HL7Utils.parseHl7String(hl7));
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
