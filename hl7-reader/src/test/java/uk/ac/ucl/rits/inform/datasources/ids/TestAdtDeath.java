package uk.ac.ucl.rits.inform.datasources.ids;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;

/**
 * Test an A03 with a death indicator set.
 *
 * @author Jeremy Stein
 */
public class TestAdtDeath extends TestHl7MessageStream {
    private AdtMessage msg;

    @BeforeEach
    public void setup() throws Exception {
        msg = processSingleAdtMessage("GenericAdt/A03_death.txt");
    }

    /**
     */
    @Test
    public void testTimeOfDeath()  {
        Instant result = msg.getPatientDeathDateTime();
        assertEquals(Instant.parse("2013-02-11T08:34:56.00Z"), result);
    }

    /**
     */
    @Test
    public void testIsDead()  {
        assertTrue(msg.getPatientDeathIndicator());
    }

    @Test
    public void testDischargeDisposition() {
        assertEquals("Home", msg.getDischargeDisposition());
    }
}
