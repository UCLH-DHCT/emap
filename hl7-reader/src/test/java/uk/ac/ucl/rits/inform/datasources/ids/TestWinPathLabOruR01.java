package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test ORU RO1 from Winpath
 * @author Stef Piatek
 */
public class TestWinPathLabOruR01 extends TestHl7MessageStream {

    private LabOrderMsg processLab(String filePath, int index) {
        List<LabOrderMsg> msgs = null;
        try {
            msgs = processSingleLabOrderMsgMessage(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(msgs);
        assertTrue(msgs.size() >= index);
        return msgs.get(index);
    }

    private Map<String, LabResultMsg> getResultsByItemCode(List<LabResultMsg> labResultMsgs) {
        return labResultMsgs.stream()
                .collect(Collectors.toMap(LabResultMsg::getTestItemLocalCode, v -> v));
    }

    /**
     * Test battery code and description are correctly parsed.
     */
    @Test
    public void testTestCodes() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_text.txt", 0);
        assertEquals("NCOV", msg.getTestBatteryLocalCode());
        assertEquals("COVID19 PCR", msg.getTestBatteryLocalDescription());
    }

    /**
     * Test LabResult result status is parsed correctly
     */
    @Test
    public void testResultStatus() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_text.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("NCVS");
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
    }
    /**
     * OBX result status is unkonwn - should
     */
    @Test
    public void testResultStatusUnkonwn() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_text.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("UNKNOWN_RS");
        assertEquals(LabResultStatus.UNKNOWN, result.getResultStatus());
    }

    /**
     * Test that string values and not numeric values are set for string values.
     */
    @Test
    public void testStringResultOnlyParsed() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_text.txt", 0);
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
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("ALP");
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
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("<VAL");
        assertEquals("<", result.getResultOperator());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getNumericValue());
    }

    /**
     * Test that greater than value sets the result operator and numeric value correctly
     */
    @Test
    public void testResultOperatorGreaterThan() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get(">VAL");
        assertEquals(">", result.getResultOperator());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getNumericValue());
    }

    /**
     * Test unknown result operator should still build, but with no known numeric result and the string value giving the original value
     */
    @Test
    public void testUnknownResultOperator() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("UNKONWN_OPERATOR");
        assertTrue(result.getNumericValue().isUnknown());
        assertEquals("?7", result.getStringValue());
    }

    /**
     * Range is 35-104 -> should be parsed correctly
     */
    @Test
    public void testSimpleRange() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("ALP");
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getReferenceHigh());
        assertEquals(InterchangeValue.buildFromHl7(35.0), result.getReferenceLow());
    }

    /**
     * Range is <7.2. Upper limit should be 7.2 - lower should delete if exists
     */
    @Test
    public void testLessThanRange() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("LESS_RANGE");
        assertEquals(InterchangeValue.buildFromHl7(7.2), result.getReferenceHigh());
        assertTrue(result.getReferenceLow().isDelete());
    }

    /**
     * Range is >7.2. Lower limit should be 7.2, upper should delete if exists
     */
    @Test
    public void testGreaterThanRange() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("GREATER_RANGE");
        assertTrue(result.getReferenceHigh().isDelete());
        assertEquals(InterchangeValue.buildFromHl7(7.2), result.getReferenceLow());
    }

    /**
     * Range is ~7.2. Unparsable so should not set a range
     */
    @Test
    public void testRangeUnparsable() {
        LabOrderMsg msg = processLab("LabOrders/oru_ro1_numeric.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        LabResultMsg result = resultsByItemCode.get("UNPARSABLE_RANGE");
        assertTrue(result.getReferenceHigh().isUnknown());
        assertTrue(result.getReferenceLow().isUnknown());
    }


}
