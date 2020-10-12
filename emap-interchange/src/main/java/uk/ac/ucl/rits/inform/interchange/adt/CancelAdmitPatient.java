package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

import java.time.Instant;

/**
 * Decision to admit patient was reversed or was entered into the system in error.
 * HL7 messages: A11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CancelAdmitPatient extends AdtMessage implements AdtCancellation {
    private static final long serialVersionUID = -1327739046375342715L;
    private Instant cancelledDateTime;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
