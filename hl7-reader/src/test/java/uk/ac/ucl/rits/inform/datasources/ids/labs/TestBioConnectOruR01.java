package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabOrder and its LabResults derived BIO-CONNECT
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestBioConnectOruR01 {
    @Autowired
    private LabReader labReader;
    private final Instant resultTime = Instant.parse("2013-07-25T12:59:00Z");
    private static final String FILE_TEMPLATE = "LabOrders/bio_connect/%s.txt";

    @Test
    void testIdentifiers() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "glucose");
        assertTrue(msg.getEpicCareOrderNumber().isUnknown());
        assertEquals("40800000", msg.getMrn());
        assertEquals("9876543", msg.getLabSpecimenNumber());
        assertTrue(msg.getVisitNumber().isEmpty());
    }

    @Test
    void testSourceSystem() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "glucose");
        assertEquals("BIO-CONNECT", msg.getSourceSystem());
    }

    @Test
    void testOrderTimes() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "glucose");
        assertEquals(InterchangeValue.buildFromHl7(resultTime), msg.getSampleReceivedTime());
        assertEquals(InterchangeValue.buildFromHl7(resultTime), msg.getOrderDateTime());
        assertTrue(msg.getRequestedDateTime().isUnknown());
        assertEquals(resultTime, msg.getStatusChangeTime());
        assertEquals(resultTime, msg.getCollectionDateTime());
    }

    /**
     * Test battery code information is correctly parsed.
     */
    @Test
    void testBatteryCodes() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "glucose");
        assertEquals("Glucose", msg.getTestBatteryLocalCode());
    }

    @Test
    void testBatteryCodingSystem() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "glucose");
        assertEquals(OrderCodingSystem.BIO_CONNECT.name(), msg.getTestBatteryCodingSystem());
    }

    /**
     * Test LabResult result status is parsed correctly
     */
    @Test
    void testResultStatus() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals(LabResultStatus.INVALID_RESULT, result.getResultStatus());
    }

    @Test
    void testNumericValueParsed() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals(InterchangeValue.buildFromHl7(7.6), result.getNumericValue());
        assertEquals(ValueType.NUMERIC, result.getMimeType());
        assertEquals("=", result.getResultOperator());
    }


    @Test
    void testRangeParsed() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals(InterchangeValue.buildFromHl7(4.0), result.getReferenceLow());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getReferenceHigh());
    }

    @Test
    void testTestCodingSystem() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals("BIO-CONNECT", result.getTestItemCodingSystem());
    }

    @Test
    void testResultTime() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals(resultTime, result.getResultTime());
    }

    /**
     * Test that numeric value, and units are set
     */
    @Test
    void testUnitsSet() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals(InterchangeValue.buildFromHl7("mmol/L"), result.getUnits());
    }

    @Test
    void testAbnormalFlagPresent() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "glucose", "Glu");
        assertEquals(InterchangeValue.buildFromHl7("H"), result.getAbnormalFlag());
    }

    @Test
    void testAbnormalFlagIsNormal() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "normal_flag", "Glu");
        assertTrue(result.getAbnormalFlag().isDelete());
    }

    @Test
    void testCollectionSpecimenType() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "glucose");
        assertEquals(InterchangeValue.buildFromHl7("BLD"), msg.getSpecimenType());
    }

}
