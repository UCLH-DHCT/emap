package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

/**
 * Test ORU RO1 from Winpath
 *
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
    public void testTestCodes()  {
        LabOrderMsg msg = processLab("LabOrders/covid19test.txt", 0);
        assertEquals("NCOV", msg.getTestBatteryLocalCode());
        assertEquals("COVID19 PCR", msg.getTestBatteryLocalDescription());
    }

    /**
     * Test that string values and not numeric values are set for string values.
     */
    @Test
    public void testStringResultOnlyParsed() {
        LabOrderMsg msg = processLab("LabOrders/covid19test.txt", 0);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        assertEquals(3, labResultMsgs.size());
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        assertEquals(new HashSet<>(Arrays.asList("NCVS", "NCVP", "NCVL")), resultsByItemCode.keySet());
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
        assertEquals(4, labResultMsgs.size());
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        assertEquals(new HashSet<>(Arrays.asList("ALP", "ALB", "GRE", "ALBR")), resultsByItemCode.keySet());
        LabResultMsg result = resultsByItemCode.get("ALP");
        assertEquals( InterchangeValue.buildFromHl7(104.0), result.getNumericValue());
        assertEquals(InterchangeValue.buildFromHl7("IU/L"), result.getUnits());
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getReferenceHigh());
        assertEquals(InterchangeValue.buildFromHl7(35.0), result.getReferenceLow());

    }
}
