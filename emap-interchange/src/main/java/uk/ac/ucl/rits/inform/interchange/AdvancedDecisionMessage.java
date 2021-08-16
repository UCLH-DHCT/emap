package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Advanced Decision requests.
 * @author Anika Cawthorn
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class AdvancedDecisionMessage extends EmapOperationMessage implements Serializable {
    /**
     * Unique epic ID for advanced decision.
     */
    private Long advancedDecisionId;
    private String mrn;
    private String visitNumber;

    /**
     * Questions and answers for advanced decisions.
     */
    private Map<String, String> questions = new HashMap<>();

    /**
     * Indicates whether the advanced decision order has been cancelled by the user.
     */
    private boolean cancelled = false;

    /**
     * Indicates whether the advanced decision order has been closed due to the patient no longer being in hospital.
     */
    private boolean closedDueToDischarge = false;

    public AdvancedDecisionMessage(String sourceId, String sourceSystem, String mrn, String visitNumber) {
        setSourceMessageId(sourceId);
        setSourceSystem(sourceSystem);
        this.mrn = mrn;
        this.visitNumber = visitNumber;
    }

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor)
            throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
