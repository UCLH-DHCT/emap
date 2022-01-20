package uk.ac.ucl.rits.inform.interchange.lab;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabsMetadataMsg extends EmapOperationMessage {
    /**
     * Short code for a test or a battery.
     */
    private String shortCode;

    /**
     * Human readable description.
     */
    private String description;

    /**
     * Is this a test or a battery of tests?
     */
    private boolean isBattery;

    /**
     * Some temporal information will be needed...
     */


    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
