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
 * @author Tom Young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientInfection extends EmapOperationMessage implements Serializable, PatientConditionMessage {

    /**
     * Unique identifier of the patient in the hospital.
     */
    private String mrn;

    /**
     * Number of the hospital visit.
     */
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();

    /**
     * Infection abbreviation.
     */
    private String conditionCode;

    /**
     * Human-readable infection name.
     */
    private InterchangeValue<String> conditionName = InterchangeValue.unknown();

    /**
     * Time of the update or message carrying this information.
     */
    private Instant updatedDateTime;

    /**
     * Unique Id for a condition in EPIC.
     * If we can't get this added to the live HL7 interface when we should remove it.
     */
    private InterchangeValue<Long> epicConditionId = InterchangeValue.unknown();

    /**
     * Status of infection.
     */
    private String status;

    /**
     * Comment on an infection.
     */
    private InterchangeValue<String> comment = InterchangeValue.unknown();

    /**
     * Infection added at...
     */
    private Instant addedTime;

    /**
     * Infection resolved at...
     */
    private InterchangeValue<Instant> resolvedTime = InterchangeValue.unknown();

    /**
     * Onset of infection known at...
     */
    private InterchangeValue<LocalDate> onsetTime = InterchangeValue.unknown();

    /**
     * Effectively message type, i.e. whether to add, update or delete the condition.
     */
    private String action = "AD";

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    public boolean statusIsActive() {
        return getStatus().equals("ACTIVE");
    }
}
