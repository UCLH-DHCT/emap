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
public class FlowsheetMetadata extends EmapOperationMessage implements ObservationType {
    /**
     * The flowsheet's internal ID within the hospital.
     */
    private String flowsheetId;
    /**
     * Flowsheet's ID number on this particular interface, eg. HL7 interface ID (MPI ID).
     * Can be null in case of metadata from Caboodle.
     */
    private String interfaceId;
    /**
     * Denotes the source system of the flowsheetId field.
     */
    private FlowsheetIdSourceSystem flowsheetIdSourceSystem;

    private String name;
    private String displayName;
    private String valueType;
    private String unit;
    private String description;

    private Instant creationInstant;
    // Not guaranteed to be set depending on the source of the data.
    private Instant lastUpdatedInstant;

    /**
     * What is the data type to which this metadata message relates?
     */
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
        return flowsheetId;
    }
}
