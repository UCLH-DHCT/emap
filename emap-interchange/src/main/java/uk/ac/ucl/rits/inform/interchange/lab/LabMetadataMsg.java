package uk.ac.ucl.rits.inform.interchange.lab;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;

import java.time.Instant;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabMetadataMsg extends EmapOperationMessage {
    /**
     * Short code for a test or a battery.
     */
    private String shortCode;

    /**
     * The coding system that this shortCode belongs to.
     */
    private OrderCodingSystem codingSystem;

    /**
     * Human readable name.
     */
    private String name;

    /**
     * Is this, for example, a test or a battery of tests?
     */
    private LabsMetadataType labsMetadataType;

    /**
     * Department code.
     */
    private String labDepartment;

    /**
     * When did this mapping start existing, to the best of our knowledge?
     */
    private Instant validFrom;

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
