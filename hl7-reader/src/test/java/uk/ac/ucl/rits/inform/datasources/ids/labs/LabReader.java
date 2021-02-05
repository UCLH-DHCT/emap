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
     * @throws Hl7MessageIgnoredException if thrown during processing
     * @throws Hl7InconsistencyException  if hl7 message malformed
     */
    LabOrderMsg process(String fileTemplate, String fileName) throws Hl7MessageIgnoredException, Hl7InconsistencyException {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(fileTemplate, fileName));
        } catch (Hl7MessageIgnoredException | Hl7InconsistencyException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return (LabOrderMsg) msgs.stream().filter(msg -> (msg instanceof LabOrderMsg)).findFirst().orElseThrow();
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
     * @throws Hl7MessageIgnoredException shouldn't happen
     * @throws Hl7InconsistencyException shouldn't happen
     */
    LabResultMsg getResult(String fileTemplate, String fileName, String testLocalCode) throws Hl7MessageIgnoredException, Hl7InconsistencyException {
        LabOrderMsg msg = process(fileTemplate, fileName);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        return resultsByItemCode.get(testLocalCode);
    }
}
