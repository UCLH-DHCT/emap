package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;

import java.time.Instant;

/**
 * Registration of a patient, not an admission.
 * HL7 messages: A04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RegisterPatient extends AdtMessage {
    private static final long serialVersionUID = 6241822001508655468L;
    private Hl7Value<Instant> presentationDateTime = Hl7Value.unknown();

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
