package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Abstract class defining a patient condition message, either a 'problem' (aka. problem list) or an infection
 * @author Tom Young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class PatientConditionMessage extends EmapOperationMessage {

    private String mrn;

    /**
     * Number of the hospital visit.
     */
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();

    /**
     * Infection or problem abbreviation.
     */
    private String conditionCode;

    /**
     * Human-readable condition name.
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
     * Status of condition.
     */
    private String status;

    /**
     * Comment on a condition.
     */
    private InterchangeValue<String> comment = InterchangeValue.unknown();

    /**
     * Condition added at...
     */
    private Instant addedTime;

    /**
     * Condition resolved at...
     */
    private InterchangeValue<Instant> resolvedTime = InterchangeValue.unknown();

    /**
     * Onset of condition known at...
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
    public abstract void processMessage(EmapOperationMessageProcessor processor)
            throws EmapOperationMessageProcessingException;

    public boolean statusIsActive(){
        return status.equals("ACTIVE");
    }
}
