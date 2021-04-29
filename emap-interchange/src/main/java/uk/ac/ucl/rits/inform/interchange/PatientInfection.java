package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Interchange format of a PatientInterchange message.
 * @author Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientInfection extends EmapOperationMessage implements Serializable {
    private String mrn;

    /**
     * Infection name.
     */
    private String infection;

    /**
     * Line number for infection.
     * Allows for multiple infections per patient to be tracked separately
     */
    private InterchangeValue<Long> line = InterchangeValue.unknown();

    /**
     * Status of infection.
     */
    private InterchangeValue<String> status= InterchangeValue.unknown();

    private InterchangeValue<String> comment= InterchangeValue.unknown();

    /**
     * Infection added at...
     */
    private Instant infectionAdded;

    /**
     * Infection resolved at...
     */
    private InterchangeValue<Instant> infectionResolved= InterchangeValue.unknown();

    /**
     * Onset of infection known at...
     */
    private InterchangeValue<Instant> infectionOnset= InterchangeValue.unknown();

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

}
