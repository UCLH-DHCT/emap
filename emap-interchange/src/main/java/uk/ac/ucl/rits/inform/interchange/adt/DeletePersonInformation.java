package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Delete all demographics and all visit information for a patient.
 * messages: A29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeletePersonInformation extends AdtMessage {
    private static final long serialVersionUID = 3337977661678135438L;

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
