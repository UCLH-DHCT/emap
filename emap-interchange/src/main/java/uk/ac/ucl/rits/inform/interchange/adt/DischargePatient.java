package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.time.Instant;

/**
 * Discharge a patient.
 * HL7 messages: A03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DischargePatient extends AdtMessage implements AdmissionDateTime {
    private InterchangeValue<Instant> admissionDateTime = InterchangeValue.unknown();

    private Instant dischargeDateTime;
    private String dischargeDisposition;
    private String dischargeLocation;


    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
