package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;

/**
 * Test an A03 with a contradictory death indicator set ("N"|"" but with a time of death set).
 *
 * @author Jeremy Stein
 */
public class TestAdtDeath2 extends TestHl7MessageStream {
    private AdtMessage msg;

    @BeforeEach
    public void setup() throws Exception {
        msg = processSingleAdtMessage("Adt/A03_death_2.txt");
    }

    /**
     * Although time of death was given in the HL7 message, death indicator is false
     * so this shouldn't be filled out.
     */
    @Test
    public void testTimeOfDeath()  {
        assertEquals(Instant.parse("2013-02-11T08:34:56.00Z"), msg.getPatientDeathDateTime());
    }

    /**
     * They shouldn't be dead.
     */
    @Test
    public void testIsNotDead()  {
        assertFalse(msg.getPatientDeathIndicator());
    }
}
