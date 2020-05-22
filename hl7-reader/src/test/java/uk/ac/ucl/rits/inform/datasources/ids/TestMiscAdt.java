package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Test some basic things about different ADT messages. Most of the fields are already
 * tested in TestAdt so no real need to do them all again here.
 */
public class TestMiscAdt extends TestHl7MessageStream {
    /**
     * A04 basics.
     */
    @Test
    public void testOutpatientRegistration() throws Exception {
        AdtMessage msg = processSingleAdtMessage("GenericAdt/A04.txt");
        assertEquals("O", msg.getPatientClass());
        // A04 is considered the same sort of event as A01, although the patient class
        // is usually different.
        assertEquals(AdtOperationType.ADMIT_PATIENT, msg.getOperationType());
        assertEquals("Adt:ADMIT_PATIENT", msg.getMessageType());
    }

    /**
     * A01 basics.
     */
    @Test
    public void testInpatientAdmission() throws Exception {
        AdtMessage msg = processSingleAdtMessage("GenericAdt/A01.txt");
        assertEquals("I", msg.getPatientClass());
        assertEquals(AdtOperationType.ADMIT_PATIENT, msg.getOperationType());
        assertEquals("Adt:ADMIT_PATIENT", msg.getMessageType());
    }
}
