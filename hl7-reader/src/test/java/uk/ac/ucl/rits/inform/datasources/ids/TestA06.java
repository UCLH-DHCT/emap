package uk.ac.ucl.rits.inform.datasources.ids;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Test a de-identified A06 (Outpatient -> Inpatient transfer) message.
 * @author Jeremy Stein
 *
 */
public class TestA06 extends TestHl7MessageStream {
    public TestA06() {
    }

    private AdtMessage msg;

    @BeforeEach
    public void setup() throws Exception {
        msg = processSingleAdtMessage("Adt/A06.txt");
    }

    /**
     * A06 (and A07) are treated like a transfer.
     */
    @Test
    public void testOperationType()  {
        assertEquals(AdtOperationType.TRANSFER_PATIENT, msg.getOperationType());
    }

    /**
     * Outpatient -> Inpatient, would be weird if this were anything but "I".
     */
    @Test
    public void testPatientClass()  {
        assertEquals("I", msg.getPatientClass());
    }

    @Test
    public void testEventOccurredTime()  {
        assertEquals(Instant.parse("2020-04-01T21:22:21.000Z"), msg.getEventOccurredDateTime());
    }

    /**
     * New location. Can (but doesn't have to) differ to the patient's previous known location.
     */
    @Test
    public void testLocation()  {
        assertEquals("T06C^T06C SR41^SR41-41", msg.getFullLocationString());
    }
}
