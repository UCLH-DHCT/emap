package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

/**
 * Swap two patients' locations.
 * Contains information about two patients locations that should be swapped.
 * From looking at epic messages, only applies to open messages and always has admission date time.
 * Locations given are the final locations for each visit after the swap has occured.
 * HL7 messages: A17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SwapLocations extends AdtMessage {
    private String otherMrn;
    private String otherNhsNumber;
    private String otherVisitNumber;
    private InterchangeValue<String> otherFullLocationString = InterchangeValue.unknown();

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
