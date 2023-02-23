package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.TestHl7MessageStream;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reads HL7 files for lab processing and provides helper methods.
 * @author Stef Piatek
 */
@Component
public class LabReader extends TestHl7MessageStream {

    /**
     * Process HL7 message and return the first LabOrderMsg
     * @param fileTemplate template for filepath with relative path from resources root for hl7 message.
     * @param fileName filename to add to template
     * @return LabOrderMsg
     * @throws Exception if thrown during processing
     */
    LabOrderMsg getFirstOrder(String fileTemplate, String fileName) throws Exception {
        return getAllOrders(fileTemplate, fileName).get(0);
    }

    List<LabOrderMsg> getAllOrders(String fileTemplate, String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(fileTemplate, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof LabOrderMsg))
                .map(o -> (LabOrderMsg) o)
                .collect(Collectors.toList());
    }

    Map<String, LabResultMsg> getResultsByItemCode(List<LabResultMsg> labResultMsgs) {
        return labResultMsgs.stream()
                .collect(Collectors.toMap(LabResultMsg::getTestItemLocalCode, v -> v));
    }

    /**
     * Process lab order and get single result
     * @param fileTemplate template for filepath with relative path from resources root for hl7 message.
     * @param fileName filename to add to template
     * @param testLocalCode local test code for result to get
     * @return lab result message
     * @throws Exception shouldn't happen
     */
    LabResultMsg getResult(String fileTemplate, String fileName, String testLocalCode) throws Exception {
        LabOrderMsg msg = getFirstOrder(fileTemplate, fileName);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        return resultsByItemCode.get(testLocalCode);
    }
}
