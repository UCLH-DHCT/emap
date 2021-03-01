package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

/**
 * Represent a flowsheet message.
 * @author Sarah Keating & Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Flowsheet extends EmapOperationMessage {
    private String mrn = "";

    private String visitNumber = "";

    /**
     * Application that the message is sent from (e.g. caboodle or EPIC)
     */
    private String sourceApplication = "";

    /**
     * Identifier used by caboodle/EPIC for the flowsheet.
     */
    private String flowsheetId = "";

    /**
     * Is the data type numeric, if false then data type is String.
     */
    private Boolean isNumericType;

    /**
     * Numeric value.
     */
    private InterchangeValue<Double> numericValue = InterchangeValue.unknown();

    /**
     * String value.
     */
    private InterchangeValue<String> stringValue = InterchangeValue.unknown();

    /**
     * Comment.
     */
    private InterchangeValue<String> comment = InterchangeValue.unknown();

    /**
     * Unit of numeric value.
     */
    private InterchangeValue<String> unit = InterchangeValue.unknown();

    /**
     * Time of the observation.
     */
    private Instant observationTime;

    /**
     * Time that the panel of observations was updated.
     */
    private Instant updatedTime;

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
