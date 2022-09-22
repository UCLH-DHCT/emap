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
        return labReader.processSingleMessage(String.format(FILE_TEMPLATE, fileName)).stream()
                .filter(msg -> msg instanceof LabOrderMsg)
                .map(o -> (LabOrderMsg) o).findFirst()
                .orElseThrow();
    }

    /**
     * Given that an imaging messages has multiple `&GDT` results with an opinion `&IMP` section
     * When the message is parsed
     * The last `&GDT` set of results should be returned as the report text
     */
    @Test
    void testMultipleResultsWithOpinion() throws Exception {
        LabOrderMsg labOrder = getLabOrder("oru_r01_imaging_multiple_results");
        String textResult = labOrder.getLabResultMsgs().stream()
                .filter(result -> "TEXT".equals(result.getTestItemLocalCode()))
                .map(LabResultMsg::getStringValue)
                .map(InterchangeValue::get)
                .findFirst().orElseThrow();

        assertEquals("Study Date: 16/1/22\nmore data\nend of report", textResult);
    }

}
