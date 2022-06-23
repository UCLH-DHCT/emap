package uk.ac.ucl.rits.inform.interchange;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

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
public class PatientProblem extends PatientConditionMessage implements Serializable {
    /**
     * Comment on an infection.
     */
    private InterchangeValue<String> comment = InterchangeValue.unknown();

    /**
     * Call back to the processor, so it knows what type this object is (i.e. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
