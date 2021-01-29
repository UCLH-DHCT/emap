package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabOrder and its LabResults derived from ORU R30 from ABL 90 flex
 * @author Stef Piatek
 */
public class TestAblOruR30Parsing extends TestHl7MessageStream {
    private final Instant resultTime = Instant.parse("2013-02-11T10:00:52Z");
    private final Instant messageTime = Instant.parse("2013-07-29T09:41:00Z");

    private LabOrderMsg processLab(String filePath) {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(msgs);
        assertFalse(msgs.isEmpty());
        return (LabOrderMsg) msgs.get(0);
    }

    private Map<String, LabResultMsg> getResultsByItemCode(List<LabResultMsg> labResultMsgs) {
        return labResultMsgs.stream()
                .collect(Collectors.toMap(LabResultMsg::getTestItemLocalCode, v -> v));
    }

    private LabResultMsg getLabResult(String filepath, String testLocalCode) {
        LabOrderMsg msg = processLab(filepath);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        return resultsByItemCode.get(testLocalCode);
    }

    @Test
    public void testIdentifiers() {
        LabOrderMsg msg = processLab("LabOrders/abl90_flex/venous.txt");
        assertNull(msg.getEpicCareOrderNumber());
        assertEquals("40800000", msg.getMrn());
        assertEquals("12345006210113012345", msg.getLabSpecimenNumber());
        assertEquals("123412341234", msg.getVisitNumber());
    }

    @Test
    void testSourceSystem() {
        LabOrderMsg msg = processLab("LabOrders/abl90_flex/venous.txt");
        assertEquals("ABL90 FLEX Plus", msg.getSourceSystem());
    }

    @Test
    void testOrderTimes() {
        LabOrderMsg msg = processLab("LabOrders/abl90_flex/venous.txt");
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
    public void testBatteryCodes() {
        LabOrderMsg msg = processLab("LabOrders/abl90_flex/venous.txt");
        assertEquals("VBG", msg.getTestBatteryLocalCode());
        assertEquals("", msg.getTestBatteryLocalDescription());
        assertEquals("ABL90 FLEX Plus", msg.getTestBatteryCodingSystem());
    }

    @Test
    public void testBatteryCodingSystem() {
        LabOrderMsg msg = processLab("LabOrders/abl90_flex/venous.txt");
        assertEquals("ABL90 FLEX Plus", msg.getTestBatteryCodingSystem());
    }

    /**
     * Test LabResult result status is parsed correctly
     */
    @Test
    public void testResultStatus() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pCO2");
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
    }


    @Test
    public void testTestCodingSystem() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pCO2");
        assertEquals("ABL90 FLEX Plus", result.getTestItemCodingSystem());
    }

    @Test
    public void testResultTime() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pCO2");
        assertEquals(resultTime, result.getResultTime());
    }

    /**
     * Test that numeric value, and units are set
     */
    @Test
    public void testUnitsSet() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pCO2");
        assertEquals(InterchangeValue.buildFromHl7("kPa"), result.getUnits());
    }

    @Test
    public void testNotes() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pO2");
        InterchangeValue<String> expected = InterchangeValue.buildFromHl7("Value below reference range");
        assertEquals(expected, result.getNotes());
    }

    @Test
    void testAbnormalFlagPresent() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pO2");
        assertEquals(InterchangeValue.buildFromHl7("L"), result.getAbnormalFlag());
    }

    @Test
    void testAbnormalFlagIsNormal() {
        LabResultMsg result = getLabResult("LabOrders/abl90_flex/venous.txt", "pH");
        assertTrue(result.getAbnormalFlag().isDelete());
    }

    /* Notes for parsing of ORU R30
    - Not parsing Notes on OBR
    - To test:
        - Collection information
        -
     */


}
