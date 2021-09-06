package uk.ac.ucl.rits.inform.interchange.visit_observations;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * A message to tell Star the matching HL7 vs Clarity flowsheet IDs.
 *
 * @author Jeremy Stein
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FlowsheetMpiMetadata extends EmapOperationMessage {
    private String hl7Id;
    private String lineNumberForFlowsheetToHl7NameMap;
    private String flowsheetRowID;
    private String unitConversion;
    private String unit;
    private Instant creationInstant;

    public FlowsheetMpiMetadata() {
    }

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
