package uk.ac.ucl.rits.inform.datasources.ids;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test a de-identified A06 (Outpatient -> Inpatient transfer) message.
 * @author Jeremy Stein
 */
public class TestA06 extends TestHl7MessageStream {
    public TestA06() {
    }

    private AdtMessage msg;

    @BeforeEach
    public void setup() throws Exception {
        msg = processSingleAdtMessage("Adt/generic/A06.txt");
    }

    /**
     * A06 (and A07) are treated like a transfer.
     */
    @Test
    public void testAdtChildClass() {
        assertTrue(msg instanceof TransferPatient);
    }

    /**
     * Outpatient -> Inpatient, would be weird if this were anything but "I".
     */
    @Test
    public void testPatientClass() {
        assertEquals("I", msg.getPatientClass().get());
    }

    @Test
    public void testEventOccurredTime() {
        assertEquals(Instant.parse("2020-04-01T21:22:21.000Z"), msg.getEventOccurredDateTime());
    }

    /**
     * New location. Can (but doesn't have to) differ to the patient's previous known location.
     */
    @Test
    public void testLocation() {
        assertEquals("T06C^T06C SR41^SR41-41", msg.getFullLocationString().get());
    }
}
