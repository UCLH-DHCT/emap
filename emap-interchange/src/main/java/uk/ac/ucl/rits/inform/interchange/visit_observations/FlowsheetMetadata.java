package uk.ac.ucl.rits.inform.interchange.visit_observations;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

/**
 * A message to tell Star that the flowsheet described is available on the
 * source system.
 *
 * @author Jeremy Stein
 * @author Anika Cawthorn
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FlowsheetMetadata extends EmapOperationMessage implements ObservationType {
    /**
     * This is the identifier for HL7 messages.
     */
    private String interfaceId;
    /**
     * The flowsheet's internal ID within the hospital.
     */
    private String flowsheetId;

    private String name;
    private String displayName;
    private String valueType;
    private String unit;
    private String description;

    private Instant creationInstant;
    /**
     * Not guaranteed to be set depending on the source of the data. In cases were no information is available,
     * the lastUpdatedInstant is populated with creationInstant.
     */
    private Instant lastUpdatedInstant;

    /**
     * The data type to which this metadata message relates.
     */
    private String sourceObservationType = "flowsheet";

    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
