package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabOrders derived from Winpath
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestWinPathOrders {
    @Autowired
    private LabReader labReader;
    private static final String FILE_TEMPLATE = "LabOrders/winpath/%s.txt";

    @Test
    void testOrmO01NWTimes() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        // order datetime is the most recent updated time
        Instant orderDateTime = Instant.parse("2013-07-28T07:08:06Z");
        assertEquals(Instant.parse("2013-07-28T07:27:00Z"), order.getCollectionDateTime());
        assertEquals(orderDateTime, order.getStatusChangeTime());
        assertEquals(InterchangeValue.buildFromHl7(orderDateTime), order.getOrderDateTime());
        assertEquals(InterchangeValue.buildFromHl7(Instant.parse("2013-07-28T07:08:00Z")), order.getRequestedDateTime());
        assertTrue(order.getSampleReceivedTime().isUnknown());
    }

    @Test
    void testOrmO01NWLabNumbers() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        assertEquals("91393667", order.getEpicCareOrderNumber());
        assertEquals("13U444444", order.getLabSpecimenNumber());
    }


    @Test
    void testOrmO01NWOrderInfo() throws Exception {
        LabOrderMsg order = labReader.process(FILE_TEMPLATE, "orm_o01_nw");
        assertEquals("Not in Message", order.getSourceSystem());
        assertEquals(InterchangeValue.buildFromHl7("BLD"), order.getSpecimenType());
        assertEquals("Lab", order.getLabDepartment());
        assertEquals("MG", order.getTestBatteryLocalCode());
        assertEquals(OrderCodingSystem.WIN_PATH.name(), order.getTestBatteryCodingSystem());
        assertTrue(order.getOrderStatus().isEmpty());
    }

}
