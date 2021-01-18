package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.LabResultMsg;

/**
 * Test an A03 with a death indicator set.
 *
 * @author Jeremy Stein
 */
public class TestCovid19Test extends TestHl7MessageStream {
    private LabOrderMsg msg;

    @BeforeEach
    public void setup() throws Exception {
        List<LabOrderMsg> msgs = processSingleLabOrderMsgMessage("LabOrders/covid19test.txt");
        assertEquals(1, msgs.size());
        msg = msgs.get(0);
    }

    /**
     */
    @Test
    public void testTestCodes()  {
        System.out.println(msg.toString());
        assertEquals("NCOV", msg.getTestBatteryLocalCode());
        assertEquals("COVID19 PCR", msg.getTestBatteryLocalDescription());
    }

    /**
     */
    @Test
    public void testResults() {
        List<LabResultMsg> LabResultMsgs = msg.getLabResultMsgs();
        assertEquals(3, LabResultMsgs.size());
        Map<String, LabResultMsg> resultsByItemCode = LabResultMsgs.stream()
                .collect(Collectors.toMap(LabResultMsg::getTestItemLocalCode, v -> v));
        assertEquals(new HashSet<>(Arrays.asList("NCVS", "NCVP", "NCVL")), resultsByItemCode.keySet());
        LabResultMsg ncvs = resultsByItemCode.get("NCVS");
        LabResultMsg ncvp = resultsByItemCode.get("NCVP");
        LabResultMsg ncvl = resultsByItemCode.get("NCVL");

        Assertions.assertTrue(ncvs.getNumericValue().isUnknown());
        assertEquals("CTNS", ncvs.getStringValue());

        Assertions.assertTrue(ncvp.getNumericValue().isUnknown());
        assertEquals("NOT detected", ncvp.getStringValue());

        Assertions.assertTrue(ncvl.getNumericValue().isUnknown());
        // why are the leading spaces on each repeat (line) being trimmed?
        assertEquals("Please note that this test was performed using\n" + "the Hologic Panther Fusion Assay.\n"
                + "This new assay is currently not UKAS accredited,\n" + "but is internally verified. UKAS extension\n"
                + "to scope to include this has been submitted.", ncvl.getStringValue());
    }
}
