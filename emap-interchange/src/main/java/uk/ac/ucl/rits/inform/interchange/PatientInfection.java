package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;

/**
 * Interchange format of a PatientInterchange message.
 * @author Stef Piatek
 * @author Tom Young
 * @author Anika Cawthorn
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientInfection extends PatientConditionMessage implements Serializable {

    /**
     * Time and date condition was added at.
     */
    private Instant addedDatetime;

    /**
     * Time and date condition was added at.
     */
    private InterchangeValue<Instant> resolvedDatetime = InterchangeValue.unknown();

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
