package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@ActiveProfiles("test")
public class TestHL7ParsingMatchesInterchangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    private void testAdtMessage(String adtFileStem) throws Exception {
        System.out.println("Testing ADT message with stem:" + adtFileStem);
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("Adt/" + adtFileStem + ".txt");
        AdtMessage expectedAdtMessage = interchangeFactory.getAdtMessage(adtFileStem + ".yaml");
        assertEquals(1, messagesFromHl7Message.size());
        assertEquals(expectedAdtMessage, messagesFromHl7Message.get(0));
    }

    @Test
    public void testGenericAdtA01() throws Exception {
        testAdtMessage("generic/A01");
        testAdtMessage("generic/A01_b");

    }

    @Test
    public void testGenericAdtA02() throws Exception {
        testAdtMessage("generic/A02");
    }

    @Test
    public void testGenericAdtA03() throws Exception {
        testAdtMessage("generic/A03");
        testAdtMessage("generic/A03_death");
        testAdtMessage("generic/A03_death_2");
        testAdtMessage("generic/A03_death_3");

    }

    @Test
    public void testGenericAdtA04() throws Exception {
        testAdtMessage("generic/A04");
    }

    @Test
    public void testGenericAdtA06() throws Exception {
        testAdtMessage("generic/A06");
    }

    @Test
    public void testGenericAdtA08() throws Exception {
        testAdtMessage("generic/A08_v1");
        testAdtMessage("generic/A08_v2");
    }

    @Test
    public void testGenericAdtA11() throws Exception {
        testAdtMessage("generic/A11");
    }
    @Test
    public void testGenericAdtA13() throws Exception {
        testAdtMessage("generic/A13");
    }

    @Test
    public void testGenericAdtA17() throws Exception {
        testAdtMessage("generic/A17");
    }

    @Test
    public void testGenericAdtA29() throws Exception {
        testAdtMessage("generic/A29");
    }

    @Test
    public void testGenericAdtA40() throws Exception {
        testAdtMessage("generic/A40");
    }
    @Test
    public void testGenericAdtA45() throws Exception {
        testAdtMessage("generic/A45");
    }
    @Test
    public void testGenericAdtA47() throws Exception {
        testAdtMessage("generic/A47");
    }

    @Test
    public void testDoubleA01WithA13() throws Exception {
        testAdtMessage("DoubleA01WithA13/A03");
        testAdtMessage("DoubleA01WithA13/A03_2");
        testAdtMessage("DoubleA01WithA13/A13");
        testAdtMessage("DoubleA01WithA13/FirstA01");
        testAdtMessage("DoubleA01WithA13/SecondA01");
    }

    @Test
    public void testPathologyIncrementalLoad() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processPathologyHl7AndFilterToPathologyOrders("PathologyOrder/Incremental.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("incremental.yaml", "0000000042");
        assertEquals(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testPathologyIncrementalDuplicateResultSegment() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processPathologyHl7AndFilterToPathologyOrders("PathologyOrder/PathologyDuplicateResultSegment.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("incremental_duplicate_result_segment.yaml", "0000000042");
        assertEquals(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testPathologyOrder() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("PathologyOrder/ORU_R01.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("ORU_R01.yaml", "0000000042");
        assertEquals(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testPathologyOrderProducesAdtFirst() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage("PathologyOrder/ORU_R01.txt").get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage("FromNonAdt/pathology_oru_r01.yaml");
        assertEquals(expectedAdt, messageFromHl7);
    }

    @Test
    public void testPathologySensitivity() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("PathologyOrder/Sensitivity.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("sensitivity.yaml", "0000000042");
        assertEquals(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testVitalSigns() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("VitalSigns/MixedHL7Message.txt");
        List<VitalSigns> expectedOrders = interchangeFactory.getVitalSigns("hl7.yaml", "0000000042");
        assertEquals(expectedOrders, messagesFromHl7Message);
    }
}
