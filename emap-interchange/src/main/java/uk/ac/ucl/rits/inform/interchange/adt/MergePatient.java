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
public class MergePatient extends AdtMessage implements PreviousIdentifiers {
    /**
     * MRN to be merged and retired.
     */
    private String previousMrn;
    private String previousNhsNumber;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
