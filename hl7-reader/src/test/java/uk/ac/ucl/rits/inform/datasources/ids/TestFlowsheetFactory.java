package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
class TestFlowsheetFactory {
    @Autowired
    private FlowsheetFactory flowsheetFactory;
    private List<Flowsheet> flowsheets;
    private Flowsheet firstFlowsheet;

    @BeforeEach
    void setUp() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MixedHL7Message.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        flowsheets = flowsheetFactory.getMessages("42", hl7Msg);
        firstFlowsheet = flowsheets.get(0);
    }

    @Test
    void testMRN() {
        for (Flowsheet vitalSign : flowsheets) {
            String result = vitalSign.getMrn();
            assertEquals("40800000", result);
        }
    }

    @Test
    void testVisitNumber() {
        for (Flowsheet vitalSign : flowsheets) {
            String result = vitalSign.getVisitNumber();
            assertEquals("123412341234", result);
        }
    }

    @Test
    void testVitalSignIdentifier() {
        String result = firstFlowsheet.getFlowsheetId();
        assertEquals("5", result);
    }

    @Test
    void testNumericValue() {
        Double result = flowsheets.get(1).getNumericValue().get();
        assertEquals(102.2, result);
    }

    @Test
    void testStringValue() {
        String result = firstFlowsheet.getStringValue().get();
        assertEquals("140/90", result);
    }

    @Test
    void testComment() {
        String result = flowsheets.get(2).getComment().get();
        assertEquals("patient was running really fast (on a hamster wheel)", result);
    }

    @Test
    void testResultStatusFtoValue() {
        // result status is 'F' so value should be saved
        String result = firstFlowsheet.getStringValue().get();
        Assertions.assertNotNull(result);
    }

    @Test
    void testResultStatusCtoValue() {
        // result status is 'C' so should be saved
        String result = flowsheets.get(4).getStringValue().get();
        Assertions.assertNotNull(result);
    }

    @Test
    void testResultStatusDtoDelete() {
        // result status is 'D' so should be converted to DELETE
        InterchangeValue<Double> numericValue = flowsheets.get(5).getNumericValue();
        InterchangeValue<String> stringValue = flowsheets.get(5).getStringValue();

        Assertions.assertTrue(numericValue.isUnknown());
        Assertions.assertTrue(stringValue.isDelete());
    }

    @Test
    void testUnit() {
        String result = flowsheets.get(3).getUnit().get();
        assertEquals("%", result);
    }

    @Test
    void testObservationTimeTaken() {
        Instant result = firstFlowsheet.getObservationTime();
        assertEquals(Instant.parse("2020-01-22T14:03:00.00Z"), result);
    }

    @Test
    void testSourceMessageId() {
        String result = firstFlowsheet.getSourceMessageId();
        assertEquals("42$01", result);
    }

    @Test
    void testMissingValue() {
        assertEquals(8, flowsheets.size());
    }

    @Test
    void testMultiLineStringValue() {
        String result = flowsheets.get(6).getStringValue().get();
        assertEquals("Supplemental Oxygen\nextra line", result);
    }

    @Test
    void testMultipleOBRs() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MultiOBR.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        flowsheets = flowsheetFactory.getMessages("42", hl7Msg);
        assertEquals(4, flowsheets.size());
    }

    /**
     * Date of 20200601 should be parsed to local date.
     * @throws Exception shouldn't happen
     */
    @Test
    void tesBSTDateValue() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/datetime_parsing.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        flowsheets = flowsheetFactory.getMessages("42", hl7Msg);
        Flowsheet msg = flowsheets.get(1);
        LocalDate expected = LocalDate.parse("2020-06-01");

        assertEquals(InterchangeValue.buildFromHl7(expected), msg.getDateValue());
    }
}
