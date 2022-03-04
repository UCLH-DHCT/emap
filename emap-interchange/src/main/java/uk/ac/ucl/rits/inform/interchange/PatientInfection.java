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
public class PatientInfection extends EmapOperationMessage implements Serializable, PatientConditionMessage {
    private String mrn;
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();

    /**
     * Infection abbreviation.
     */
    private String infectionCode;

    /**
     * Human-readable infection name.
     */
    private InterchangeValue<String> infectionName = InterchangeValue.unknown();

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

    @Override
    public String getCode() {
        return infectionCode;
    }

    @Override
    public InterchangeValue<String> getName() {
        return infectionName;
    }

    @Override
    public InterchangeValue<Long> getEpicId() {
        return epicInfectionId;
    }

    @Override
    public Instant getAddedTime() {
        return infectionAdded;
    }

    @Override
    public InterchangeValue<Instant> getResolvedTime() {
        return infectionResolved;
    }

    @Override
    public InterchangeValue<LocalDate> getOnsetTime() {
        return infectionOnset;
    }

    @Override
    public String getAction() {
        return "AD";
    }
}
