package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Change patient identifiers
 * HL7 messages: A47
 * The new MRN should not already exist in the system. Therefore not a merge, but instead change the MRN string value to the new value.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChangePatientIdentifiers extends AdtMessage implements PreviousIdentifiers {
    private static final long serialVersionUID = 2764828119416263690L;

    private String previousMrn;
    private String previousNhsNumber;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
