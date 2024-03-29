package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.TestHl7MessageStream;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabOrders derived from Bank Manager
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestBankManagerParsing extends TestHl7MessageStream {
    @Autowired
    private static final String FILE_TEMPLATE = "LabOrders/bank_manager/%s.txt";

    @Autowired
    private LabReader labReader;

    private static final OrderCodingSystem CODING_SYSTEM = OrderCodingSystem.BANK_MANAGER;
    private String labOrder = "19B44444";
    private Instant collectionTime = Instant.parse("2013-07-29T05:18:00Z");

    @Test
    void testOrderTimes() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_order");
        assertEquals(collectionTime, order.getCollectionDateTime());
        assertNotNull(order.getStatusChangeTime());
        assertEquals(InterchangeValue.buildFromHl7(collectionTime), order.getOrderDateTime());
        assertEquals(InterchangeValue.buildFromHl7(collectionTime), order.getSampleReceivedTime());
        assertTrue(order.getRequestedDateTime().isUnknown());
    }

    @Test
    void testLabNumbers() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_order");
        assertEquals(labOrder, order.getLabSpecimenNumber());
        assertTrue(order.getEpicCareOrderNumber().isUnknown());
    }

    @Test
    void testOrderInfo() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_order");
        assertEquals("Not in Message", order.getSourceSystem());
        assertEquals("GPS", order.getTestBatteryLocalCode());
        assertEquals(CODING_SYSTEM.name(), order.getTestBatteryCodingSystem());
        assertTrue(order.getQuestions().isEmpty());
        assertEquals("I", order.getResultStatus());
        assertEquals(CODING_SYSTEM.name(), order.getLabDepartment());
        assertNull(order.getOrderStatus());
        assertTrue(order.getSpecimenType().isUnknown());
    }

    @Test
    void testIdentifiers() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_order");
        assertEquals("40800000", order.getMrn());
        assertEquals("1013420255", order.getVisitNumber());
    }

    /**
     * BMCOMMENT notes should be parsed
     * @throws Exception shouldn't happen
     */
    @Test
    void testClinicalInformation() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_bmcomment");
        assertEquals(InterchangeValue.buildFromHl7("Clinical Note"), order.getClinicalInformation());
    }

    @Test
    void testResultTimes() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_result");
        assertEquals(collectionTime, order.getCollectionDateTime());
        assertNotNull(order.getStatusChangeTime());
        assertEquals(InterchangeValue.buildFromHl7(collectionTime), order.getOrderDateTime());
        assertEquals(InterchangeValue.buildFromHl7(collectionTime), order.getSampleReceivedTime());
        assertTrue(order.getRequestedDateTime().isUnknown());
    }

    @Test
    void testSingleStringResult() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_r01_result", "GPS1");
        assertEquals(InterchangeValue.buildFromHl7("O NEG"), result.getStringValue());
    }

    @Test
    void testStringResultAdjacent() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_r01_result", "GPS1");
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
        assertEquals(ValueType.TEXT, result.getMimeType());
        assertEquals(OrderCodingSystem.BANK_MANAGER.name(), result.getTestItemCodingSystem());
    }

    @Test
    void testResultComments() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_r01_result_comment", "GPS2");
        assertEquals(InterchangeValue.buildFromHl7("Anti-D"), result.getNotes());
    }

    @Test
    void testCancelOrder() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_cancel");
        assertTrue(order.getEpicCareOrderNumber().isDelete());
    }

    /**
     * HL7 message with no ORC ID should be skipped, so no order messages should be created.
     * @throws Exception shouldn't happen
     */
    @Test
    void testOrderWithNoOrcId() throws Exception {
        List<LabOrderMsg> order = labReader.getAllOrders(FILE_TEMPLATE, "oru_r01_no_orc");
        assertTrue(order.isEmpty());
    }

}
