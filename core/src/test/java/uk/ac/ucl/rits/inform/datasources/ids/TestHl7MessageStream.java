package uk.ac.ucl.rits.inform.datasources.ids;

import org.springframework.test.context.ActiveProfiles;

import ca.uhn.hl7v2.model.Message;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;

/**
 * Base class for writing tests which take HL7 message(s) as input, and check
 * the correctness of the resultant interchange message(s) (eg. AdtMessage).
 */
@ActiveProfiles("test")
public abstract class TestHl7MessageStream {

    protected AdtMessage processSingleMessage(String resourceFileName) throws Exception {
        String hl7 = HL7Utils.readHl7FromResource(resourceFileName);
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        return new AdtMessageBuilder(hl7Msg, "42").getAdtMessage();
    }
}
