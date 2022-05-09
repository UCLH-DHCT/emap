package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

/**
 * Pending transfer event.
 * Implemented: ADT A15 (Pending transfer)
 * Not implemented: ADT A14 (Pending admit), ADT A16 (Pending discharge)
 * @author Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PendingTransfer extends AdtMessage implements PendingEvent {
    private PendingType pendingEventType = PendingType.TRANSFER;
    private InterchangeValue<String> pendingLocation = InterchangeValue.unknown();

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
