package uk.ac.ucl.rits.inform.interchange;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Interchange format of a PatientProblem message. In hospital terminology they are referred to as problem lists.
 * <p>
 * PatientProblems are similar to PatientProblems in that they have a start date from which they have been diagnosed
 * and they can change (be updated or deleted) over time.
 * @author Anika Cawthorn
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientProblem extends PatientConditionMessage implements Serializable {

    /**

     * Call back to the processor, so it knows what type this object is (i.e. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    // Below getters and setters are to maintain backwards compatibility
    public String getProblemCode() {
        return getConditionCode();
    }
    public void setProblemCode(String problemCode) {
        setConditionCode(problemCode);
    }

    public InterchangeValue<String> getProblemName() {
        return getConditionName();
    }
    public void setProblemName(InterchangeValue<String> problemName) {
        setConditionName(problemName);
    }

    public InterchangeValue<Long> getEpicProblemId() {
        return getEpicConditionId();
    }
    public void setEpicProblemId(InterchangeValue<Long> epicProblemId) {
        setEpicConditionId(epicProblemId);
    }

    public InterchangeValue<Instant> getProblemResolved() {
        return getResolvedTime();
    }
    public void setProblemResolved(InterchangeValue<Instant> resolvedTime) {
        setResolvedTime(resolvedTime);
    }

    public Instant getProblemAdded() {
        return getAddedTime();
    }
    public void setProblemAdded(Instant addedTime) {
        setAddedTime(addedTime);
    }

    public InterchangeValue<LocalDate> getProblemOnset() {
        return getOnsetTime();
    }
    public void setProblemOnset(InterchangeValue<LocalDate> date) {
        setOnsetTime(date);
    }
}
