package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class TestFlowsheetFactory {
    @Autowired
    private FlowsheetFactory flowsheetFactory;
    private Message hl7Msg;
    private List<Flowsheet> flowsheets;
    private Flowsheet firstFlowsheet;

    @BeforeEach
    void setUp() throws Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSigns/MixedHL7Message.txt");
        hl7Msg = HL7Utils.parseHl7String(hl7);
        flowsheets = flowsheetFactory.getMessages("42", hl7Msg);
        firstFlowsheet = flowsheets.get(0);
    }


    /**
     * To test that specific errors are thrown, build a flowsheet from the OBX segment with the identifier name.
     * @param msg            hl7 message
     * @param identifierName OBX identifier name to use
     * @throws HL7Exception              shouldn't happen
     * @throws Hl7InconsistencyException if we can't parse the OBX segment
     */
    private void buildIndividualFlowsheetFromOBX(Message msg, String identifierName) throws HL7Exception, Hl7InconsistencyException {
        ORU_R01_PATIENT_RESULT patientResult = ((ORU_R01) msg).getPATIENT_RESULT();
        PID pid = patientResult.getPATIENT().getPID();
        MSH msh = (MSH) msg.get("MSH");
        PV1 pv1 = patientResult.getPATIENT().getVISIT().getPV1();
        EVN evn = (EVN) msg.get("EVN");
        Instant recordedDateTime = HL7Utils.interpretLocalTime(evn.getRecordedDateTime());

        ORU_R01_OBSERVATION observation = patientResult.getORDER_OBSERVATION().getOBSERVATIONAll()
                .stream()
                .filter(obs -> hasObservationName(identifierName, obs))
                .findFirst().orElseThrow();
        flowsheetFactory.buildFlowsheet("id", observation, msh, pid, pv1, recordedDateTime);
    }

    private boolean hasObservationName(String identifierName, ORU_R01_OBSERVATION obs) {
        return identifierName.equals(obs.getOBX().getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty());
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
        String result = firstFlowsheet.getFlowsheetRowEpicId();
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
    void testMultipleOBRs() throws Exception {
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

    /**
     * Given that an hl7 message has OBX segments which are malformed
     * When each specific OBX segment is processed
     * Then an exception should be thrown
     * @param inconsistentIdentifier identifier to select from the hl7 message, supplied by static method
     */
    @ParameterizedTest
    @MethodSource("inconsistentHl7")
    void testInconsistentHl7(String inconsistentIdentifier) {
        assertThrows(Hl7InconsistencyException.class,
                () -> buildIndividualFlowsheetFromOBX(hl7Msg, inconsistentIdentifier));
    }


    /**
     * @return stream of OBX identifiers which have hl7 inconsistencies
     */
    static Stream<String> inconsistentHl7() {
        return Stream.of(
                // result status issues
                "Empty result status", "Unexpected result status",
                // no values
                "String No value", "Numeric no value", "Date no value",
                // unrecognised type, though at some point will add this in
                "Country 1 (starting with most recent)"
        );
    }
}
