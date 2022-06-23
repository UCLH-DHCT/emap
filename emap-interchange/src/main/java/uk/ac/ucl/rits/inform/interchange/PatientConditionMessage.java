package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Interface defining a patient condition message, either a 'problem' (aka. problem list) or an infection
 * @author Tom Young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class PatientConditionMessage extends EmapOperationMessage {

    private String mrn;

    /**
     * Status of the condition.
     */
    private String status;

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
     * Unique Id for a condition, e.g. problem list id in clarity.
     */
    private InterchangeValue<Long> conditionId = InterchangeValue.unknown();

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
    private ConditionAction action = ConditionAction.ADD;

    /**
     * Identifier for condition as provided in HL7 messages.
     */
    private InterchangeValue<Long> epicConditionId = InterchangeValue.unknown();

    /**
     * Comment for condition as provided in HL7 messages.
     */
    private InterchangeValue<String> comment = InterchangeValue.unknown();
}
