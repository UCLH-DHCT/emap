package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;

/**
 * Swap two patients' locations.
 * Contains information about two patients locations that should be swapped.
 * From looking at epic messages, only applies to open messages and always has admission date time.
 * HL7 messages: A17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SwapLocations extends AdtMessage {
    private static final long serialVersionUID = 1811745798929900545L;

    private String otherMrn;
    private String otherNhsNumber;
    private String otherVisitNumber;
    private Hl7Value<String> otherFullLocationString = Hl7Value.unknown();

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
