package uk.ac.ucl.rits.inform.interchange.visit_observations;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * A message to tell Star that the flowsheet described is available on the
 * source system.
 *
 * @author Jeremy Stein
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FlowsheetMetadata extends EmapOperationMessage implements  ObservationType {
    private String flowsheetRowEpicId;
    private String name;
    private String displayName;
    private String valueType;
    private String unit;
    private String description;
    private Instant creationInstant;
    private Instant lastUpdatedInstant;
    private String sourceObservationType = "flowsheet";

    public FlowsheetMetadata() {
    }

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    /**
     * @return Id of observation in application.
     */
    @Override
    @JsonIgnore
    public String getId() {
        return flowsheetRowEpicId;
    }
}
