package uk.ac.ucl.rits.inform.interchange;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Interchange format of a PatientProblem message. In hospital terminology they are referred to as problem lists.
 * <p>
 * PatientProblems are similar to PatientInfections in that they have a start date from which they have been diagnosed
 * and they can change (be updated or deleted) over time.
 * @author Anika Cawthorn
 * @author Tom Young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientProblem extends EmapOperationMessage implements Serializable, PatientConditionMessage {

    /**
     * Unique identifier of the patient in the hospital.
     */
    private String mrn;

    /**
     * Number of the hospital visit.
     */
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();

    /**
     * Problem abbreviation.
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
     * Status of problem.
     */
    private String status;

    /**
     * Comment on a problem.
     */
    private InterchangeValue<String> comment = InterchangeValue.unknown();

    /**
     * Problem added at...
     */
    private Instant addedTime;

    /**
     * Problem resolved at...
     */
    private InterchangeValue<Instant> resolvedTime = InterchangeValue.unknown();

    /**
     * Onset of problem known at...
     */
    private InterchangeValue<LocalDate> onsetTime = InterchangeValue.unknown();

    /**
     * Effectively message type, i.e. whether to add, update or delete the condition.
     */
    private String action = "AD";

    /**
     * Call back to the processor, so it knows what type this object is (i.e. double dispatch).
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
