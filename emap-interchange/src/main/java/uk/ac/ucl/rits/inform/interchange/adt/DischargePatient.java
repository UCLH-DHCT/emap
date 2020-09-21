package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

import java.time.Instant;

/**
 * Discharge a patient.
 * HL7 messages: A03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DischargePatient extends AdtMessage {
    private static final long serialVersionUID = -1528594767815651653L;

    private Instant dischargeDateTime;
    private String dischargeDisposition;
    private String dischargeLocation;


    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
