package uk.ac.ucl.rits.inform.interchange;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Interchange format of a ProblemList message.
 * @author Anika Cawthorn
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class ProblemList extends EmapOperationMessage implements Serializable {
    private String mrn;
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();
    /**
     * Problem code.
     */
    private String problemCode;
    /**
     * Human-readable problem name.
     */
    private InterchangeValue<String> problemName = InterchangeValue.unknown();

    /**
     * Time of the update or message carrying this information.
     */
    private Instant updatedDateTime;

    /**
     * Unique Id for problem in EPIC.
     */
    private InterchangeValue<Long> epicProblemId = InterchangeValue.unknown();
    /**
     * Problem added at...
     */
    private Instant problemAdded;

    /**
     * Problem resolved at...
     */
    private InterchangeValue<Instant> problemResolved = InterchangeValue.unknown();

    /**
     * Onset of Problem known at...
     */
    private InterchangeValue<LocalDate> problemOnset = InterchangeValue.unknown();

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
