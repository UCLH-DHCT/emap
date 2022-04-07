package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Interchange format of a PatientInterchange message.
 * @author Stef Piatek
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientInfection extends PatientConditionMessage implements Serializable {

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    // Below getters and setters are to maintain backwards compatibility
    public String getInfectionCode() {
        return getConditionCode();
    }
    public void setInfectionCode(String infectionCode) {
        setConditionCode(infectionCode);
    }

    public InterchangeValue<String> getInfectionName() {
        return getConditionName();
    }
    public void setInfectionName(InterchangeValue<String> infectionName) {
        setConditionName(infectionName);
    }

    public InterchangeValue<Long> getEpicInfectionId() {
        return getEpicConditionId();
    }
    public void setEpicInfectionId(InterchangeValue<Long> epicInfectionId) {
        setEpicConditionId(epicInfectionId);
    }

    public InterchangeValue<Instant> getInfectionResolved() {
        return getResolvedTime();
    }
    public void setInfectionResolved(InterchangeValue<Instant> resolvedTime) {
        setResolvedTime(resolvedTime);
    }

    public Instant getInfectionAdded() {
        return getAddedTime();
    }
    public void setInfectionAdded(Instant addedTime) {
        setAddedTime(addedTime);
    }

    public InterchangeValue<LocalDate> getInfectionOnset() {
        return getOnsetTime();
    }
    public void setInfectionOnset(InterchangeValue<LocalDate> date) {
        setOnsetTime(date);
    }
}
