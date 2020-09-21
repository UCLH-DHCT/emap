package uk.ac.ucl.rits.inform.datasources.ids;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;


/**
 * Test an A03 with a death indicator set.
 *
 * @author Jeremy Stein
 */
public class TestAdtDeath extends TestHl7MessageStream {
    private AdtMessage msg;

    @BeforeEach
    public void setup() throws Exception {
        msg = processSingleAdtMessage("Adt/generic/A03_death.txt");
    }

    /**
     */
    @Test
    public void testTimeOfDeath()  {

        Instant result = msg.getPatientDeathDateTime().get();
        assertEquals(Instant.parse("2013-02-11T08:34:56.00Z"), result);
    }

    /**
     */
    @Test
    public void testIsDead()  {
        assertTrue(msg.getPatientDeathIndicator().get());
    }

    @Test
    public void testDischargeDisposition() {
        DischargePatient dischargePatient = (DischargePatient) msg;
        assertEquals("Home", dischargePatient.getDischargeDisposition());
    }
}
