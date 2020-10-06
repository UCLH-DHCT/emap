package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Move visit from previous MRN and visit number to current.
 * HL7 messages: A45
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MoveVisitInformation extends AdtMessage implements PreviousIdentifiers {

    private static final long serialVersionUID = 8612053846611150031L;

    private String previousMrn;
    private String previousNhsNumber;
    private String previousVisitNumber;

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
