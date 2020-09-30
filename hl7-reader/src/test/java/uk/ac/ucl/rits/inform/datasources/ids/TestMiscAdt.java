package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
        AdtMessage msg = processSingleAdtMessage("Adt/generic/A04.txt");
        assertEquals(PatientClass.OUTPATIENT, msg.getPatientClass().get());
        // A04 is considered the same sort of event as A01, although the patient class
        // is usually different.
        assertEquals(RegisterPatient.class.getName(), msg.getMessageType());
    }

    /**
     * A01 basics.
     */
    @Test
    public void testInpatientAdmission() throws Exception {
        AdtMessage msg = processSingleAdtMessage("Adt/generic/A01.txt");
        assertEquals(PatientClass.INPATIENT, msg.getPatientClass().get());
        assertTrue(msg instanceof AdmitPatient);
        assertEquals(AdmitPatient.class.getName(), msg.getMessageType());
    }
}
