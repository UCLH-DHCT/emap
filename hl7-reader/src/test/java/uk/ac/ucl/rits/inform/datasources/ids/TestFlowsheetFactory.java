package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
public class TestFlowsheetFactory {
    @Autowired
    private FlowsheetFactory flowsheetFactory;
    private List<Flowsheet> flowsheets;
    private Flowsheet firstFlowsheet;

    @BeforeEach
    public void setUp() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MixedHL7Message.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        flowsheets = flowsheetFactory.getMessages("42", hl7Msg);
        firstFlowsheet = flowsheets.get(0);
    }

    @Test
    public void testMRN() {
        for (Flowsheet vitalSign : flowsheets) {
            String result = vitalSign.getMrn();
            assertEquals("40800000", result);
        }
    }

    @Test
    public void testVisitNumber() {
        for (Flowsheet vitalSign : flowsheets) {
            String result = vitalSign.getVisitNumber();
            assertEquals("123412341234", result);
        }
    }

    @Test
    public void testVitalSignIdentifier() {
        String result = firstFlowsheet.getFlowsheetId();
        assertEquals("5", result);
    }

    @Test
    public void testNumericValue() {
        Double result = flowsheets.get(1).getNumericValue().get();
        assertEquals(102.2, result);
    }

    @Test
    public void testStringValue() {
        String result = firstFlowsheet.getStringValue().get();
        assertEquals("140/90", result);
    }

    @Test
    public void testComment() {
        String result = flowsheets.get(2).getComment().get();
        assertEquals("patient was running really fast (on a hamster wheel)", result);
    }

    @Test
    public void testMultipleComments() {
        String result = flowsheets.get(3).getComment().get();
        assertEquals("comment 1a\ncomment 1b\ncomment 2", result);
    }

    @Test
    public void testResultStatusFtoValue() {
        // result status is 'F' so value should be saved
        String result = firstFlowsheet.getStringValue().get();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testResultStatusCtoValue() {
        // result status is 'C' so should be saved
        String result = flowsheets.get(4).getStringValue().get();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testResultStatusDtoDelete() {
        // result status is 'D' so should be converted to DELETE
        Hl7Value<Double> numericValue = flowsheets.get(5).getNumericValue();
        Hl7Value<String> stringValue = flowsheets.get(5).getStringValue();

        Assertions.assertEquals(Hl7Value.delete(), numericValue);
        Assertions.assertEquals(Hl7Value.delete(), stringValue);
    }

    @Test
    public void testUnit() {
        String result = flowsheets.get(3).getUnit().get();
        assertEquals("%", result);
    }

    @Test
    public void testObservationTimeTaken() {
        Instant result = firstFlowsheet.getObservationTime();
        assertEquals(Instant.parse("2020-01-22T14:03:00.00Z"), result);
    }

    @Test
    public void testSourceMessageId() {
        String result = firstFlowsheet.getSourceMessageId();
        assertEquals("42$01", result);
    }

    @Test
    public void testMissingValue() {
        assertEquals(7, flowsheets.size());
    }

    @Test
    public void testMultiLineStringValue() {
        String result = flowsheets.get(6).getStringValue().get();
        assertEquals("Supplemental Oxygen\nextra line", result);
    }

    @Test
    public void testMultipleOBRs() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MultiOBR.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        flowsheets = flowsheetFactory.getMessages("42", hl7Msg);
        assertEquals(4, flowsheets.size());
    }
}
