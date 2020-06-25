package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@ActiveProfiles("test")
public class TestHL7ParsingMatchesIntechangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    public List<? extends EmapOperationMessage> parseHL7File(String resFile) throws IOException, HL7Exception, Hl7InconsistencyException {
        List<? extends EmapOperationMessage> messagesFromHl7Message = new ArrayList<>();
        Hl7InputStreamMessageIterator hl7Iter = HL7Utils.hl7Iterator(new File(HL7Utils.getPathFromResource(resFile)));
        while (hl7Iter.hasNext()) {
            Message hl7Msg = hl7Iter.next();
            messagesFromHl7Message = IdsOperations.messageFromHl7Message(hl7Msg, 0);
        }
        return messagesFromHl7Message;
    }

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
