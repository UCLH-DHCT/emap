package uk.ac.ucl.rits.inform.datasources.ids;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;

/**
 * Base class for writing tests which take HL7 message(s) as input, and check
 * the correctness of the resultant interchange message(s) (eg. AdtMessage).
 */
@ActiveProfiles("test")
public abstract class TestHl7MessageStream {

    /**
     * processSingleMessage is preferred.
     * @param resourceFileName
     * @return
     * @throws Exception
     */
    protected AdtMessage processSingleAdtMessage(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        return new AdtMessageBuilder(hl7Msg, "42").getAdtMessage();
    }

    /**
     * processSingleMessage is preferred.
     * @param resourceFileName
     * @return
     * @throws Exception
     */
    protected List<PathologyOrder> processSinglePathologyOrderMessage(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        ORU_R01 hl7Msg = (ORU_R01) HL7Utils.parseHl7String(hl7);
        return PathologyOrderBuilder.buildPathologyOrdersFromResults("42", hl7Msg);
    }

    /**
     * Convert HL7 message to one or more Interchange messages, determining the correct type.
     * @param resourceFileName
     * @return
     * @throws Exception
     */
    protected List<? extends EmapOperationMessage> processSingleMessage(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        return IdsOperations.messageFromHl7Message(hl7Msg, 42);
    }

    /**
     * Convert multiple HL7 messages into interchange format, determining the correct type
     * @param resourceFileName
     * @return
     * @throws Exception
     */
    protected List<? extends EmapOperationMessage> processMultiplePathologyOrderMessages(String resourceFileName) throws Exception {
        Hl7InputStreamMessageIterator hl7Iter = HL7Utils.hl7Iterator(new File(HL7Utils.getPathFromResource(resourceFileName)));
        List<PathologyOrder> messagesFromHl7Message = new ArrayList<>();
        while (hl7Iter.hasNext()) {
            Message hl7Msg = hl7Iter.next();
            messagesFromHl7Message.addAll((List<PathologyOrder>) IdsOperations.messageFromHl7Message(hl7Msg, 42));
        }
        return messagesFromHl7Message;
    }
}
