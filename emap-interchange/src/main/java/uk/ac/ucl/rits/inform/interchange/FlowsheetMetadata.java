package uk.ac.ucl.rits.inform.interchange;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
public class FlowsheetMetadata extends EmapOperationMessage {
    private String flowsheetRowEpicId;
    private String name;
    private String displayName;
    private String abbreviation;
    private String valueType;
    private String rowType;
    private String unit;
    private String description;
    private Boolean isBloodLoss;
    private Boolean isCalculated;
    private Instant creationInstant;
    private Instant lastUpdatedInstant;
    private Boolean isInferred;
    private Boolean isDeleted;

    public FlowsheetMetadata() {
    }

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

}
