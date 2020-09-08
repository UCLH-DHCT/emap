package uk.ac.ucl.rits.inform.interchange.adt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * Inpatient, outpatient or emergency admission.
 * HL7 messages: A01, A04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AdmitPatient extends AdtMessage {
    private static final long serialVersionUID = -4310475980149363358L;

    @Override
    public String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        return processor.processMessage(this);
    }
}
