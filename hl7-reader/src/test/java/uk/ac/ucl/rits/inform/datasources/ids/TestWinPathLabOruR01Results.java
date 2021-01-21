package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabResults derived from ORU RO1 from Winpath
 * @author Stef Piatek
 */
public class TestWinPathLabOruR01Results extends TestHl7MessageStream {

    private LabOrderMsg processLab(String filePath) {
        List<LabOrderMsg> msgs = null;
        try {
            msgs = processSingleLabOrderMsgMessage(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(msgs);
        assertFalse(msgs.isEmpty());
        return msgs.get(0);
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

    /**
     * Test battery code and description are correctly parsed.
     */
    @Test
    public void testTestCodes() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_text.txt");
        assertEquals("NCOV", msg.getTestBatteryLocalCode());
        assertEquals("COVID19 PCR", msg.getTestBatteryLocalDescription());
    }

    /**
     * Test LabResult result status is parsed correctly
     */
    @Test
    public void testResultStatus() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_text.txt", "NCVS");
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
    }

    /**
     * OBX result status is unkonwn - should
     */
    @Test
    public void testResultStatusUnkonwn() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_text.txt", "UNKNOWN_RS");
        assertEquals(LabResultStatus.UNKNOWN, result.getResultStatus());
    }

    /**
     * Test that string values and not numeric values are set for string values.
     */
    @Test
    public void testStringResultOnlyParsed() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_text.txt");
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg ncvs = resultsByItemCode.get("NCVS");
        LabResultMsg ncvp = resultsByItemCode.get("NCVP");
        LabResultMsg ncvl = resultsByItemCode.get("NCVL");

        assertTrue(ncvs.getNumericValue().isUnknown());
        assertEquals("CTNS", ncvs.getStringValue());

        assertTrue(ncvp.getNumericValue().isUnknown());
        assertEquals("NOT detected", ncvp.getStringValue());

        assertTrue(ncvl.getNumericValue().isUnknown());
        // why are the leading spaces on each repeat (line) being trimmed?
        assertEquals(new StringBuilder()
                .append("Please note that this test was performed using\n")
                .append("the Hologic Panther Fusion Assay.\n")
                .append("This new assay is currently not UKAS accredited,\n")
                .append("but is internally verified. UKAS extension\n")
                .append("to scope to include this has been submitted.").toString(), ncvl.getStringValue());
    }


    /**
     * Test that numeric value, units, range are all set in simple calse
     */
    @Test
    public void testNumericSimplePath() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "ALP");
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getNumericValue());
        assertEquals(InterchangeValue.buildFromHl7("IU/L"), result.getUnits());
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getReferenceHigh());
        assertEquals(InterchangeValue.buildFromHl7(35.0), result.getReferenceLow());
    }

    /**
     * Test that less than value sets the result operator and numeric value correctly
     */
    @Test
    public void testResultOperatorLessThan() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "<VAL");
        assertEquals("<", result.getResultOperator());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getNumericValue());
    }

    /**
     * Test that greater than value sets the result operator and numeric value correctly
     */
    @Test
    public void testResultOperatorGreaterThan() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", ">VAL");
        assertEquals(">", result.getResultOperator());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getNumericValue());
    }

    /**
     * Test unknown result operator should still build, but with no known numeric result and the string value giving the original value
     */
    @Test
    public void testUnknownResultOperator() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "UNKONWN_OPERATOR");
        assertTrue(result.getNumericValue().isUnknown());
        assertEquals("?7", result.getStringValue());
    }

    /**
     * Range is 35-104 -> should be parsed correctly
     */
    @Test
    public void testSimpleRange() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "ALP");
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getReferenceHigh());
        assertEquals(InterchangeValue.buildFromHl7(35.0), result.getReferenceLow());
    }

    /**
     * Range is <7.2. Upper limit should be 7.2 - lower should delete if exists
     */
    @Test
    public void testLessThanRange() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "LESS_RANGE");
        assertEquals(InterchangeValue.buildFromHl7(7.2), result.getReferenceHigh());
        assertTrue(result.getReferenceLow().isDelete());
    }

    /**
     * Range is >7.2. Lower limit should be 7.2, upper should delete if exists
     */
    @Test
    public void testGreaterThanRange() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "GREATER_RANGE");
        assertTrue(result.getReferenceHigh().isDelete());
        assertEquals(InterchangeValue.buildFromHl7(7.2), result.getReferenceLow());
    }

    /**
     * Range is 0-2-7.2. Unparsable so should not set a range
     */
    @Test
    public void testRangeUnparsable() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "UNPARSABLE_RANGE");
        assertTrue(result.getReferenceHigh().isUnknown());
        assertTrue(result.getReferenceLow().isUnknown());
    }

    @Test
    public void testNotes() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "SFOL");
        InterchangeValue<String> expected = InterchangeValue.buildFromHl7("Folate result assumes no folic acid supplement\non day of sampling");
        assertEquals(expected, result.getNotes());
    }

    @Test
    void testAbnormalFlagPresent() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "VB12");
        assertEquals(InterchangeValue.buildFromHl7("H"), result.getAbnormalFlags());
    }

    @Test
    void testAbnormalFlagAbsent() {
        LabResultMsg result = getLabResult("LabOrders/oru_ro1_numeric.txt", "ALP");
        assertTrue(result.getAbnormalFlags().isDelete());
    }


}
