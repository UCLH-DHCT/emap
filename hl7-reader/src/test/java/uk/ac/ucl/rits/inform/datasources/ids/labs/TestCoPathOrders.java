package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabOrders derived from Winpath
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestCoPathOrders {
    @Autowired
    private LabReader labReader;
    public static final String CODING_SYSTEM = OrderCodingSystem.CO_PATH.name();
    private static final String FILE_TEMPLATE = "LabOrders/co_path/%s.txt";
    private String epicOrder = "12121212";
    private String labOrder = "UH20-4444";
    private String batteryCode = "UC";
    private Instant collectionTime = Instant.parse("2013-07-28T23:20:00Z");

    @Test
    void testQuestionHasMultipleLinesAndSeparator() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        Pair<String, String> questionAndAnswer = order.getQuestions().get(1);
        assertEquals("How many labels to print?", questionAndAnswer.getLeft());
        assertEquals("2 \nthis will test \nmulti-line and -> separator", questionAndAnswer.getRight());
    }

    @Test
    void testSampleCollectionMethod() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sc");
        assertEquals(InterchangeValue.buildFromHl7("FNA (CYTOlogy Use) - ri"), order.getCollectionMethod());
    }


    @Test
    void testOrmO01NWTimes() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        assertEquals(collectionTime, order.getCollectionDateTime());
        assertNotNull(order.getStatusChangeTime());
        assertEquals(order.getStatusChangeTime(), order.getOrderDateTime().get());
        assertTrue(order.getRequestedDateTime().isSave());

        assertTrue(order.getSampleReceivedTime().isUnknown());
    }

    @Test
    void testOrmO01NWLabNumbers() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        assertEquals(labOrder, order.getLabSpecimenNumber());
        assertEquals(InterchangeValue.buildFromHl7(epicOrder), order.getEpicCareOrderNumber());
    }

    @Test
    void testOrmO01NWOrderInfo() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        assertEquals("Not in Message", order.getSourceSystem());
        assertEquals(InterchangeValue.buildFromHl7("PLF"), order.getSpecimenType());
        assertEquals("Path,Cyt", order.getLabDepartment());
        assertEquals(batteryCode, order.getTestBatteryLocalCode());
        assertEquals(CODING_SYSTEM, order.getTestBatteryCodingSystem());
        assertFalse(order.getQuestions().isEmpty());
        Pair<String, String> questionAndAnswer = order.getQuestions().get(0);
        assertEquals("Clinical Details:", questionAndAnswer.getLeft());
        assertEquals("Previous lymphoma, new pleaural effusion, spinal ostemyelitis, trachstomy in situ", questionAndAnswer.getRight());
        assertTrue(order.getOrderStatus().isEmpty());
    }

    @Test
    void testOrmO01SCTimes() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sc");
        assertEquals(collectionTime, order.getCollectionDateTime());
        assertNotNull(order.getStatusChangeTime());
        assertTrue(order.getSampleReceivedTime().isSave());

        assertTrue(order.getOrderDateTime().isUnknown());
        assertTrue(order.getRequestedDateTime().isUnknown());
    }

    @Test
    void testOrmO01SCLabNumbers() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sc");
        assertEquals(InterchangeValue.buildFromHl7(epicOrder), order.getEpicCareOrderNumber());
        assertEquals(labOrder, order.getLabSpecimenNumber());
    }

    @Test
    void testOrmO01SCOrderInfo() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sc");
        assertEquals("CoPath", order.getSourceSystem());
        assertEquals("UC", order.getLabDepartment());
        assertEquals(batteryCode, order.getTestBatteryLocalCode());
        assertEquals(CODING_SYSTEM, order.getTestBatteryCodingSystem());
        assertEquals("CM", order.getOrderStatus());
        assertEquals("I", order.getResultStatus());
        assertTrue(order.getCollectionMethod().isSave());

        assertTrue(order.getSpecimenType().isUnknown());
    }


    @Test
    void testOrmO01SNTimes() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sn");
        assertEquals(collectionTime, order.getCollectionDateTime());
        assertNotNull(order.getStatusChangeTime());
        assertEquals(order.getStatusChangeTime(), order.getOrderDateTime().get());

        assertTrue(order.getRequestedDateTime().isUnknown());
        assertTrue(order.getSampleReceivedTime().isUnknown());
    }

    @Test
    void testOrmO01SNLabNumbers() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sn");
        assertEquals(labOrder, order.getLabSpecimenNumber());

        assertTrue(order.getEpicCareOrderNumber().isUnknown());
    }

    @Test
    void testOrmO01SNOrderInfo() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_sn");
        assertEquals("CoPath", order.getSourceSystem());
        assertEquals("UC", order.getLabDepartment());
        assertEquals(batteryCode, order.getTestBatteryLocalCode());
        assertEquals(CODING_SYSTEM, order.getTestBatteryCodingSystem());
        assertEquals("IP", order.getOrderStatus());
        assertEquals("I", order.getResultStatus());

        assertTrue(order.getSpecimenType().isUnknown());
    }

    @Test
    void testOrrR01MessagesThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "orr_o02"));
    }

}
