package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

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
    private String visitNumber;

    /**
     * Infection name.
     */
    private String infection;

    /**
     * Time of the update or message carrying this information.
     */
    private Instant updatedDateTime;

    /**
     * Unique Id for infection in EPIC.
     * If we can't get this added to the live HL7 interface when we should remove it.
     */
    private InterchangeValue<Long> epicInfectionId = InterchangeValue.unknown();

    /**
     * Status of infection.
     */
    private InterchangeValue<String> status = InterchangeValue.unknown();

    private InterchangeValue<String> comment = InterchangeValue.unknown();

    /**
     * Infection added at...
     */
    private Instant infectionAdded;

    /**
     * Infection resolved at...
     */
    private InterchangeValue<Instant> infectionResolved = InterchangeValue.unknown();

    /**
     * Onset of infection known at...
     */
    private InterchangeValue<LocalDate> infectionOnset = InterchangeValue.unknown();

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
