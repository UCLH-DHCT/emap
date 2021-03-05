package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabOrder and its LabResults derived from ORU R30 from ABL 90 flex
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestAblOruR30Parsing {
    @Autowired
    private LabReader labReader;
    private final Instant resultTime = Instant.parse("2013-02-11T10:00:52Z");
    private static final String FILE_TEMPLATE = "LabOrders/abl90_flex/%s.txt";


    @Test
    void testIdentifiers() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "unit");
        assertTrue(msg.getEpicCareOrderNumber().isUnknown());
        assertEquals("40800000", msg.getMrn());
        assertEquals("12345006210113012345", msg.getLabSpecimenNumber());
        assertEquals("123412341234", msg.getVisitNumber());
    }

    @Test
    void testSourceSystem() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "unit");
        assertEquals("ABL90 FLEX Plus", msg.getSourceSystem());
    }

    @Test
    void testOrderTimes() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "unit");
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
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "unit");
        assertEquals("VBG", msg.getTestBatteryLocalCode());
    }

    @Test
    void testBatteryCodingSystem() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "unit");
        assertEquals(OrderCodingSystem.ABL90_FLEX_PLUS.name(), msg.getTestBatteryCodingSystem());
    }

    /**
     * Test LabResult result status is parsed correctly
     */
    @Test
    void testResultStatus() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pCO2");
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
    }

    @Test
    void testNumericValueForced() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pCO2");
        assertEquals(InterchangeValue.buildFromHl7(5.82), result.getNumericValue());
        assertEquals("NM", result.getValueType());
        assertEquals("=", result.getResultOperator());
    }

    @Test
    void testTestCodingSystem() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pCO2");
        assertEquals("ABL90 FLEX Plus", result.getTestItemCodingSystem());
    }

    @Test
    void testResultTime() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pCO2");
        assertEquals(resultTime, result.getResultTime());
    }

    /**
     * Test that numeric value, and units are set
     */
    @Test
    void testUnitsSet() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pCO2");
        assertEquals(InterchangeValue.buildFromHl7("kPa"), result.getUnits());
    }

    @Test
    void testNotes() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pO2");
        InterchangeValue<String> expected = InterchangeValue.buildFromHl7("Value below reference range");
        assertEquals(expected, result.getNotes());
    }

    @Test
    void testAbnormalFlagPresent() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pO2");
        assertEquals(InterchangeValue.buildFromHl7("L"), result.getAbnormalFlag());
    }

    @Test
    void testAbnormalFlagIsNormal() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "pH");
        assertTrue(result.getAbnormalFlag().isDelete());
    }

    @Test
    void testCollectionSpecimenType() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "unit");
        assertEquals(InterchangeValue.buildFromHl7("Venous"), msg.getSpecimenType());
    }

    @Test
    void testProficiencyTestingSamplesSkipped() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "ignored"));
    }

    @Test
    void testCollectionTimeRequired() {
        assertThrows(Hl7InconsistencyException.class, () -> labReader.process(FILE_TEMPLATE, "no_collection_time"));
    }

    @Test
    void testUnparseableNumericValue() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "unit", "unparseable");
        assertTrue(result.getNumericValue().isDelete());
    }

}
