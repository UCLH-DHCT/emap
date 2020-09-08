package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Decision to discharge patient was reversed or was entered into the system in error.
 * HL7 messages: A13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CancelDischargePatient extends AdtMessage {
    private static final long serialVersionUID = 4829427394505551212L;

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
