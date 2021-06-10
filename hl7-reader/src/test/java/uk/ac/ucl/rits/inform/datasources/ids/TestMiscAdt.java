package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test some basic things about different ADT messages. Most of the fields are already
 * tested in TestAdt so no real need to do them all again here.
 */
public class TestMiscAdt extends TestHl7MessageStream {

    private final Instant BST_BIRTHDATE = Instant.parse("1925-07-04T00:00:00Z");
    private final Instant GMT_BIRTHDATE = Instant.parse("1980-01-01T00:00:00Z");
    private final Duration PARSED_TIME = Duration.parse("PT03H44M01S");

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

    /**
     * When a birth datetime is purely a date, should keep the original day, even when BST.
     * @throws Exception shouldn't happen
     */
    @Test
    void testBSTBirthDate() throws Exception {
        AdtMessage msg = processSingleAdtMessage("Adt/birth_date_bst.txt");
        assertEquals(InterchangeValue.buildFromHl7(BST_BIRTHDATE), msg.getPatientBirthDate());
    }

    /**
     * When a birth datetime has time information, should be converted to UTC.
     * @throws Exception shouldn't happen
     */
    @Test
    void testBSTBirthDateTime() throws Exception {
        AdtMessage msg = processSingleAdtMessage("Adt/birth_datetime_bst.txt");
        assertEquals(InterchangeValue.buildFromHl7(BST_BIRTHDATE.plus(PARSED_TIME)), msg.getPatientBirthDate());
    }

    /**
     * When a birth datetime is purely a date, should keep original day.
     * @throws Exception shouldn't happen
     */
    @Test
    void testGMTBirthDate() throws Exception {
        AdtMessage msg = processSingleAdtMessage("Adt/birth_date_gmt.txt");
        assertEquals(InterchangeValue.buildFromHl7(GMT_BIRTHDATE), msg.getPatientBirthDate());
    }

    /**
     * When a birth datetime has time information in GMT, should keep original datetime as already in UTC.
     * @throws Exception shouldn't happen
     */
    @Test
    void testGMTBirthDateTime() throws Exception {
        AdtMessage msg = processSingleAdtMessage("Adt/birth_date_gmt.txt");
        assertEquals(InterchangeValue.buildFromHl7(GMT_BIRTHDATE), msg.getPatientBirthDate());
    }
}
