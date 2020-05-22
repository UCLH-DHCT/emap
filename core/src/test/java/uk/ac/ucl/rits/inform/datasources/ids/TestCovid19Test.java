package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.PathologyResult;

/**
 * Test an A03 with a death indicator set.
 *
 * @author Jeremy Stein
 */
public class TestCovid19Test extends TestHl7MessageStream {
    private PathologyOrder msg;

    @BeforeEach
    public void setup() throws Exception {
        List<PathologyOrder> msgs = processSinglePathologyOrderMessage("covid19test.txt");
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
        List<PathologyResult> pathologyResults = msg.getPathologyResults();
        assertEquals(3, pathologyResults.size());
        Map<String, PathologyResult> resultsByItemCode = pathologyResults.stream()
                .collect(Collectors.toMap(PathologyResult::getTestItemLocalCode, v -> v));
        assertEquals(new HashSet<>(Arrays.asList("NCVS", "NCVP", "NCVL")), resultsByItemCode.keySet());
        PathologyResult ncvs = resultsByItemCode.get("NCVS");
        PathologyResult ncvp = resultsByItemCode.get("NCVP");
        PathologyResult ncvl = resultsByItemCode.get("NCVL");

        assertNull(ncvs.getNumericValue());
        assertEquals("CTNS", ncvs.getStringValue());

        assertNull(ncvp.getNumericValue());
        assertEquals("NOT detected", ncvp.getStringValue());

        assertNull(ncvl.getNumericValue());
        // why are the leading spaces on each repeat (line) being trimmed?
        assertEquals("Please note that this test was performed using\n" + "the Hologic Panther Fusion Assay.\n"
                + "This new assay is currently not UKAS accredited,\n" + "but is internally verified. UKAS extension\n"
                + "to scope to include this has been submitted.", ncvl.getStringValue());
    }
}
