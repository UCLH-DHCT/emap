package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.labs.LabParser;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for writing tests which take HL7 message(s) as input, and check
 * the correctness of the resultant interchange message(s) (eg. AdtMessage).
 */
@ActiveProfiles("test")
@SpringBootTest
public abstract class TestHl7MessageStream {
    @Autowired
    private AdtMessageFactory adtMessageFactory;
    @Autowired
    private IdsOperations idsOperations;

    /**
     * processSingleMessage is preferred.
     * @param resourceFileName filename containing the HL7 message
     * @return interchange message
     * @throws Exception if message malformed
     */
    protected AdtMessage processSingleAdtMessage(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        return adtMessageFactory.getAdtMessage(hl7Msg, "42");
    }

    /**
     * processSingleMessage is preferred.
     * @param resourceFileName filename containing the HL7 message
     * @return interchange messages
     * @throws Exception if message malformed
     */
    protected List<LabOrderMsg> processSingleWinPathOruR01(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        ORU_R01 hl7Msg = (ORU_R01) HL7Utils.parseHl7String(hl7);
        return LabParser.buildLabOrders("42", hl7Msg);
    }

    /**
     * Convert HL7 message to one or more Interchange messages, determining the correct type.
     * @param resourceFileName filename containing the HL7 message
     * @return interchange messages
     * @throws Exception if message malformed
     */
    protected List<? extends EmapOperationMessage> processSingleMessage(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        List<? extends EmapOperationMessage> messages = idsOperations.messageFromHl7Message(hl7Msg, 42);
        return messages;
    }

    /**
     * Convert multiple HL7 lab order messages and then filter out Adt messages.
     * @param resourceFileName filename containing the HL7 message
     * @return interchange messages
     * @throws Exception if message malformed
     */
    protected List<? extends EmapOperationMessage> processLabHl7AndFilterToLabOrderMsgs(String resourceFileName) throws Exception {
        return processMultipleLabHl7Messages(resourceFileName).stream().filter(msg -> !(msg instanceof AdtMessage)).collect(Collectors.toList());
    }

    /**
     * Process a single HL7 message into multiple interchange messages, and then filter out ADT messages.
     * @param resourceFileName filename containing the HL7 message
     * @return interchange messages
     * @throws Exception if message malformed
     */
    protected List<? extends EmapOperationMessage> processSingleMessageAndRemoveAdt(String resourceFileName) throws Exception {
        return processSingleMessage(resourceFileName).stream().filter(msg -> !(msg instanceof AdtMessage)).collect(Collectors.toList());
    }

    /**
     * Process multiple lab order HL7 messages within one text file
     * @param resourceFileName filename containing the HL7 message
     * @return interchange messages
     * @throws Exception if message malformed
     */
    protected List<EmapOperationMessage> processMultipleLabHl7Messages(String resourceFileName) throws Exception {
        Hl7InputStreamMessageIterator hl7Iter = HL7Utils.hl7Iterator(new File(HL7Utils.getPathFromResource(resourceFileName)));
        List<EmapOperationMessage> messagesFromHl7Message = new ArrayList<>();
        while (hl7Iter.hasNext()) {
            Message hl7Msg = hl7Iter.next();
            messagesFromHl7Message.addAll(idsOperations.messageFromHl7Message(hl7Msg, 42));
        }
        return messagesFromHl7Message;
    }
}
