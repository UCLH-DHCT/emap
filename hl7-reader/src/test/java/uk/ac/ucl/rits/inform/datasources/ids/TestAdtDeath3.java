package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;

/**
 * Test an A03 with a death indicator set to N.
 *
 * @author Jeremy Stein
 */
public class TestAdtDeath3 extends TestHl7MessageStream {
    private AdtMessage msg;

    @Before
    public void setup() throws Exception {
        msg = processSingleMessage("GenericAdt/A03_death_3.txt");
    }

    @Test
    public void testTimeOfDeath()  {
        assertNull(msg.getPatientDeathDateTime());
    }

    @Test
    public void testIsDead()  {
        assertFalse(msg.getPatientDeathIndicator());
    }
}
