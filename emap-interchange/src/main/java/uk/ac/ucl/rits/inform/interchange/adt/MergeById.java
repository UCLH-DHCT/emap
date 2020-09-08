package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Merge the entire record of two patients.
 * HL7 messages: A40
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MergeById extends AdtMessage {
    private static final long serialVersionUID = -2500473433999508161L;

    private String mergedPatientId;

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
