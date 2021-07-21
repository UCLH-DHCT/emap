package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Lab results derived from Winpath
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestCoPathResults {
    @Autowired
    private LabReader labReader;
    private static final String FILE_TEMPLATE = "LabOrders/co_path/%s.txt";

    /**
     * Test pdf report parsed as bytes, mime and local code should be set to PDF.
     * @throws Exception shouldn't happen
     */
    @Test
    void testValueAsBytes() throws Exception {
        File reportFile = new File(getClass().getResource("/LabOrders/co_path/report.pdf").getFile());
        byte[] expectedBytes;
        try (FileInputStream inputStream = new FileInputStream(reportFile)) {
            expectedBytes = inputStream.readAllBytes();
        }

        LabResultMsg result = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_copath")
                .getLabResultMsgs().stream()
                .filter(r -> r.getByteValue().isSave())
                .findFirst().orElseThrow();
        assertArrayEquals(expectedBytes, result.getByteValue().get());
        assertEquals(ValueType.PDF, result.getMimeType());
        assertEquals(ValueType.PDF.name(), result.getTestItemLocalCode());
    }

    /**
     * Multiline text report should be joined by newlines, mime and item local code should be set to text.
     * @throws Exception shouldn't happen
     */
    @Test
    void testValueAsText() throws Exception {
        String reportText = "PATIENT NAME:\n...\nREPORTED:\n26/03/2019";
        List<LabResultMsg> results = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_multi_obx_report").getLabResultMsgs();
        assertEquals(1, results.size());
        assertEquals(InterchangeValue.buildFromHl7(reportText), results.get(0).getStringValue());
        assertEquals(ValueType.TEXT, results.get(0).getMimeType());
        assertEquals(ValueType.TEXT.name(), results.get(0).getTestItemLocalCode());
    }

    /**
     * Results with message consonsistencies should be skipped.
     * @return stream of dynamic tests
     */
    @TestFactory
    Stream<DynamicTest> testHl7Inconsistencies() {
        return List.of(
                "oru_r01_id_change", "oru_r01_sub_id_change", "oru_r01_multiple_value_reps", "oru_r01_unrecognised_data_type")
                .stream()
                .map(filename -> DynamicTest.dynamicTest(
                        filename,
                        () -> {
                            LabOrderMsg orders = labReader.getFirstOrder(FILE_TEMPLATE, filename);
                            if (filename.equals("oru_r01_sub_id_change")) {
                                assertEquals(1, orders.getLabResultMsgs().size());
                            } else {
                                assertTrue(orders.getLabResultMsgs().isEmpty());
                            }
                        })
                );
    }


    /**
     * PDF report is not valid base64, corrupted data should not be added so no results with value as bytes.
     * @throws Exception shouldn't happen
     */
    @TestFactory
    Stream<DynamicTest> testCorruptBytesReport() {
        return List.of("oru_r01_report_coding_unexpected", "oru_r01_corrupt_bytes")
                .stream()
                .map(filename -> DynamicTest.dynamicTest(
                        filename,
                        () -> {
                            Optional<LabResultMsg> result = labReader.getFirstOrder(FILE_TEMPLATE, filename)
                                    .getLabResultMsgs().stream()
                                    .filter(r -> r.getByteValue().isSave())
                                    .findFirst();
                            assertTrue(result.isEmpty());
                        })
                );
    }

    /**
     * Message with string value and empty pdf report should only have single result message.
     * @throws Exception shouldn't happen
     */
    @Test
    void testEmptyPdfReport() throws Exception {
        List<LabResultMsg> results = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_empty_report").getLabResultMsgs();
        assertEquals(1, results.size());
        assertTrue(results.get(0).getStringValue().isSave());
    }

    @Test
    void testResultDateTime() throws Exception {
        LabResultMsg result = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_copathplus").getLabResultMsgs().get(0);
        assertEquals(Instant.parse("2013-07-13T08:00:00Z"), result.getResultTime());
    }

    @Test
    void testResultAdjacentFields() throws Exception {
        LabResultMsg result = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_copathplus").getLabResultMsgs().get(0);
        assertEquals(OrderCodingSystem.CO_PATH.name(), result.getTestItemCodingSystem());
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
        assertEquals("1", result.getObservationSubId());
        // other fields should be defaults
        assertTrue(result.getAbnormalFlag().isDelete());
        assertTrue(result.getNotes().isUnknown());
        assertTrue(result.getReferenceLow().isUnknown());
        assertTrue(result.getReferenceHigh().isUnknown());
        assertTrue(result.getUnits().isUnknown());
        assertNull(result.getResultOperator());
        assertNull(result.getEpicCareOrderNumber());
        assertNull(result.getLabIsolate());
    }

    /**
     * CoPathPlus message has the internal lab number in place of the epic lab number - so no epic lab number should be added to message.
     * @throws Exception shouldn't happen
     */
    @Test
    void testCoPathPlusDoesntAddEpicNumber() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_copathplus");
        assertTrue(order.getEpicCareOrderNumber().isUnknown());
    }


}
