package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;

import java.time.Instant;

/**
 * Inpatient, outpatient or emergency admission.
 * HL7 messages: A01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AdmitPatient extends AdtMessage implements AdmissionDateTime  {
    private static final long serialVersionUID = -4310475980149363358L;
    private Hl7Value<Instant> admissionDateTime = Hl7Value.unknown();


    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
