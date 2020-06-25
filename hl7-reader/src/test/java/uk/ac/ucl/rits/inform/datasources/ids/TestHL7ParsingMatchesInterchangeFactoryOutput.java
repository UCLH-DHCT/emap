package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@ActiveProfiles("test")
public class TestHL7ParsingMatchesInterchangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    @Test
    public void testAdtA01() throws Exception {
        AdtMessage messagesFromHl7Message = processSingleAdtMessage("Adt/A01.txt");
        AdtMessage expectedOrders = interchangeFactory.getAdtMessage("A01.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }


    @Test
    public void testPathologyIncrementalLoad() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processMultiplePathologyOrderMessages("PathologyOrder/Incremental.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("incremental.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }

    @Test
    public void testPathologyIncrementalDuplicateResultSegment() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processMultiplePathologyOrderMessages("PathologyOrder/PathologyDuplicateResultSegment.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("incremental_duplicate_result_segment.yaml", "0000000042");
        for (int i = 0; i < expectedOrders.size(); i++) {
            assertEquals(messagesFromHl7Message.get(i), expectedOrders.get(i));

        }
        assertEquals(messagesFromHl7Message, expectedOrders);
    }

    @Test
    public void testPathologyOrder() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("PathologyOrder/ORU_R01.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("ORU_R01.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }

    @Test
    public void testPathologySensitivity() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("PathologyOrder/Sensitivity.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("sensitivity.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }


    @Test
    public void testVitalSigns() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("VitalSigns/MixedHL7Message.txt");
        List<VitalSigns> expectedOrders = interchangeFactory.getVitalSigns("hl7.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }
}
