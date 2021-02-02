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
     * @param filePath relative path from resources root for hl7 message
     * @return LabOrderMsg
     * @throws Hl7MessageIgnoredException if thrown during processing
     * @throws Hl7InconsistencyException  if hl7 message malformed
     */
    LabOrderMsg process(String filePath) throws Hl7MessageIgnoredException, Hl7InconsistencyException {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(filePath);
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

    LabResultMsg getResult(String filepath, String testLocalCode) throws Hl7MessageIgnoredException, Hl7InconsistencyException {
        LabOrderMsg msg = process(filepath);
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = getResultsByItemCode(labResultMsgs);
        return resultsByItemCode.get(testLocalCode);
    }
}
