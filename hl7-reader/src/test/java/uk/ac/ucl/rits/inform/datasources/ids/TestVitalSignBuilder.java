package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.ResultStatus;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

@ActiveProfiles("test")
public class TestVitalSignBuilder {
    private List<VitalSigns> vitalSigns;
    private VitalSigns firstVitalSign;

    @BeforeEach
    public void setUp() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MixedHL7Message.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        vitalSigns = new VitalSignBuilder("42", (ORU_R01) hl7Msg).getMessages();
        firstVitalSign = vitalSigns.get(0);
    }

    @Test
    public void testMRN() {
        for (VitalSigns vitalSign : vitalSigns) {
            String result = vitalSign.getMrn();
            assertEquals("21014099", result);
        }
    }

    @Test
    public void testVisitNumber() {
        for (VitalSigns vitalSign : vitalSigns) {
            String result = vitalSign.getVisitNumber();
            assertEquals("1002040107", result);
        }
    }

    @Test
    public void testVitalSignIdentifier() {
        String result = firstVitalSign.getVitalSignIdentifier();
        assertEquals("EPIC$5", result);
    }

    @Test
    public void testNumericValue() {
        Double result = vitalSigns.get(1).getNumericValue();
        assertEquals(102.2, result);
    }

    @Test
    public void testStringValue() {
        String result = firstVitalSign.getStringValue();
        assertEquals("140/90", result);
    }

    @Test
    public void testComment() {
        String result = vitalSigns.get(2).getComment();
        assertEquals("patient was running really fast (on a hamster wheel)", result);
    }

    @Test
    public void testMultipleComments() {
        String result = vitalSigns.get(3).getComment();
        assertEquals("comment 1a\ncomment 1b\ncomment 2", result);
    }

    @Test
    public void testResultStatusFtoSave() {
        // result status is 'F' so should be converted to SAVE
        ResultStatus result = firstVitalSign.getResultStatus();
        assertEquals(ResultStatus.SAVE, result);
    }

    @Test
    public void testResultStatusCtoSave() {
        // result status is 'C' so should be converted to SAVE
        ResultStatus result = vitalSigns.get(4).getResultStatus();
        assertEquals(ResultStatus.SAVE, result);
    }

    @Test
    public void testResultStatusDtoDelete() {
        // result status is 'D' so should be converted to DELETE
        ResultStatus result = vitalSigns.get(5).getResultStatus();
        Double numericValue = vitalSigns.get(5).getNumericValue();
        String stringValue = vitalSigns.get(5).getStringValue();

        assertEquals(ResultStatus.DELETE, result);
        assertNull(numericValue);
        assertEquals("\"\"", stringValue);

    }

    @Test
    public void testResultStatusUnrecognisedSave() {
        // result status is 'NOT_EXPECTED' so should be converted to SAVE
        ResultStatus result = vitalSigns.get(2).getResultStatus();
        assertEquals(ResultStatus.SAVE, result);
    }

    @Test
    public void testUnit() {
        String result = vitalSigns.get(3).getUnit();
        assertEquals("%", result);
    }

    @Test
    public void testObservationTimeTaken() {
        Instant result = firstVitalSign.getObservationTimeTaken();
        assertEquals(Instant.parse("2020-01-22T14:03:00.00Z"), result);
    }

    @Test
    public void testSourceMessageId() {
        String result = firstVitalSign.getSourceMessageId();
        assertEquals("42$01", result);
    }

    @Test
    public void testMissingValue() {
        assertEquals(7, vitalSigns.size());
    }

    @Test
    public void testMultiLineStringValue() {
        String result = vitalSigns.get(6).getStringValue();
        assertEquals("Supplemental Oxygen\nextra line", result);
    }

    @Test
    public void testMultipleOBRs() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MultiOBR.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        vitalSigns = new VitalSignBuilder("42", (ORU_R01) hl7Msg).getMessages();
        assertEquals(4, vitalSigns.size());
    }

}
