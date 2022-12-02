package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Parsing of Imaging labs specific cases.
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestImagingLabs {
    @Autowired
    private LabReader labReader;
    public static final String CODING_SYSTEM = OrderCodingSystem.PACS.name();
    private static final String FILE_TEMPLATE = "LabOrders/imaging/%s.txt";

    private LabOrderMsg getLabOrder(String fileName) throws Exception {
        return labReader.processSingleMessage(String.format(FILE_TEMPLATE, fileName)).stream().filter(msg -> msg instanceof LabOrderMsg).map(o -> (LabOrderMsg) o).findFirst().orElseThrow();
    }

    /**
     * Given that an imaging messages has multiple `&GDT` results, `&ADT` and `INDICATIONS` with an opinion `&IMP` section
     * When the message is parsed
     * The last `&GDT` set of results should be returned as the report text, and only the `INDICATIONS` should be returned as results
     */
    @Test
    void testMultipleResultsWithOpinion() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        // Check the result types
        Set<String> resultTypes = labOrder.getLabResultMsgs().stream().map(LabResultMsg::getTestItemLocalCode).collect(Collectors.toSet());
        assertEquals(Set.of("TEXT", "INDICATIONS"), resultTypes);
        // Check the report text is from &GDT
        String textResult = labOrder.getLabResultMsgs().stream().filter(result -> "TEXT".equals(result.getTestItemLocalCode())).map(LabResultMsg::getStringValue).map(InterchangeValue::get).findFirst().orElseThrow();
        String expectedResult = new StringJoiner("\n")
                .add("--------")
                .add("report about an MDT summary ")
                .add("This is a summary report. The complete report is available in the patient's medical record. If you cannot access the medical record, please contact the sending organisation for a detailed fax or copy.")
                .add("").add("Study Date: 16/1/22").add("more data").add("end of report").add("")
                .add("OPINION:").add("An actionable alert has been placed on the report and the referring clinician emailed.")
                .add("Signed by:").add("Panda ORANGE").toString();
        assertEquals(expectedResult, textResult);
    }
}
