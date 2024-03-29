package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

import java.time.Instant;

/**
 * Decision to transfer patient was reversed or was entered into the system in error.
 * HL7 messages: A12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CancelTransferPatient extends AdtMessage implements AdtCancellation {
    private Instant cancelledDateTime;
    private String cancelledLocation;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
