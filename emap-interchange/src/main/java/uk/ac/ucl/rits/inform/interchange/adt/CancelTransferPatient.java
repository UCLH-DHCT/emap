package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Decision to transfer patient was reversed or was entered into the system in error.
 * HL7 messages: A12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CancelTransferPatient extends AdtMessage {
    private static final long serialVersionUID = 2821790913106604487L;

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
