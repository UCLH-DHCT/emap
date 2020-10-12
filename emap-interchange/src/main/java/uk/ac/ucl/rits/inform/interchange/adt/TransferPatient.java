package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;

import java.time.Instant;

/**
 * Transfer a patient to a different location.
 * HL7 messages: A02, A06, A07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TransferPatient extends AdtMessage implements AdmissionDateTime {
    private static final long serialVersionUID = 1811745798929900545L;
    private Hl7Value<Instant> admissionDateTime;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
