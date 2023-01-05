package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Parsing of Imaging labs specific cases.
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestImagingLabs {
    @Autowired
    private LabReader labReader;
    private static final String FILE_TEMPLATE = "LabOrders/imaging/%s.txt";

    private LabOrderMsg getLabOrder(String fileName) throws Exception {
        return labReader.processSingleMessage(String.format(FILE_TEMPLATE, fileName)).stream().filter(msg -> msg instanceof LabOrderMsg).map(o -> (LabOrderMsg) o).findFirst().orElseThrow();
    }

    private String getTextResultByTestCode(LabOrderMsg labOrder, String textCode) {
        return labOrder.getLabResultMsgs().stream().filter(result -> textCode.equals(result.getTestItemLocalCode())).map(LabResultMsg::getStringValue).map(InterchangeValue::get).findFirst().orElseThrow();
    }

    /**
     * Given that an imaging messages has multiple `&GDT` results, `&ADT` and `INDICATIONS` with an opinion `&IMP` section
     * When the message is parsed
     * Then the final test codes should be human-readable names.
     * @throws Exception shouldn't happen
     */
    @Test
    void testMultipleResultTypes() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        Set<String> resultTypes = labOrder.getLabResultMsgs().stream().map(LabResultMsg::getTestItemLocalCode).collect(Collectors.toSet());
        assertEquals(Set.of("IMPRESSION", "ADDENDA", "INDICATIONS", "NARRATIVE", "SIGNATURE"), resultTypes);
    }

    /**
     * Given a message exists with an addenda
     * When the message is parsed
     * The impression should have all addenda lines
     * @throws Exception shouldn't happen
     */
    @Test
    void testAddenda() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        String textResult = getTextResultByTestCode(labOrder, "ADDENDA");
        String expectedResult = new StringJoiner("\n")
                .add("---------------------------------------- ")
                .add("report about an MDT summary ")
                .add("Electronically Signed by: RABBIT PETER 12/12/12 ")
                .add("----------------------------------------")
                .toString();
        assertEquals(expectedResult, textResult);
    }

    /**
     * Given a message exists with an initial `&GDT` narrative, then an `&IMP` impression, and a final `&GDT` result
     * When the message is parsed
     * The final `&GDT` result which contains signing information should not have been added to the NARRATIVE
     * @throws Exception shouldn't happen
     */
    @Test
    void testNarrative() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        String textResult = getTextResultByTestCode(labOrder, "NARRATIVE");
        String expectedResult = new StringJoiner("\n")
                .add("This is a summary report. The complete report is available in the patient's medical record. If you cannot access the medical record, please contact the sending organisation for a detailed fax or copy.")
                .add("")
                .add("Study Date: 16/1/22")
                .add("more data")
                .add("end of report")
                .toString();
        assertEquals(expectedResult, textResult);
    }

    /**
     * Given a message exists with an impression
     * When the message is parsed
     * The impression should have all `IMP` lines
     * @throws Exception shouldn't happen
     */
    @Test
    void testImpression() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        String textResult = getTextResultByTestCode(labOrder, "IMPRESSION");
        String expectedResult = new StringJoiner("\n")
                .add("OPINION:")
                .add("An actionable alert has been placed on the report and the referring clinician emailed.")
                .toString();
        assertEquals(expectedResult, textResult);
    }

    /**
     * Given an imaging message which contains a signature
     * When processed
     * The signature should be parsed from the last 3 lines of the message
     * @throws Exception shouldn't happen
     */
    @Test
    void testSignature() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        String textResult = getTextResultByTestCode(labOrder, "SIGNATURE");
        String expectedResult = new StringJoiner("\n")
                .add("Signed by:")
                .add("Panda ORANGE")
                .add("16/1/22")
                .toString();
        assertEquals(expectedResult, textResult);

    }

}
