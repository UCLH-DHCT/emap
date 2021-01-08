package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * ADT message implied from a non-ADT source (such as Flowsheet or Pathology results).
 *
 * Demographics and encounter information can be used if there is no existing information, but will always be updated from a trusted source.
 * Location information from the messages is not trusted.
 *
 * @author Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImpliedAdtMessage extends AdtMessage {
    private static final long serialVersionUID = 6177251667805777164L;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
