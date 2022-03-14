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
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientProblem extends PatientConditionMessage implements Serializable{

    /**
     * Call back to the processor, so it knows what type this object is (i.e. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
