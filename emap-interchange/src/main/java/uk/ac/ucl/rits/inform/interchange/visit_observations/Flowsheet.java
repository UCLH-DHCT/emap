package uk.ac.ucl.rits.inform.interchange.visit_observations;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.ValueType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represent a flowsheet message.
 * @author Sarah Keating & Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Flowsheet extends EmapOperationMessage implements ObservationType {
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
     * Data type of value.
     * Used to determine which value field should be used
     */
    private ValueType valueType;

    /**
     * Numeric value.
     */
    private InterchangeValue<Double> numericValue = InterchangeValue.unknown();

    /**
     * String value.
     */
    private InterchangeValue<String> stringValue = InterchangeValue.unknown();

    /**
     * Date value.
     */
    private InterchangeValue<LocalDate> dateValue = InterchangeValue.unknown();

    /**
     * Time value, currently not used.
     */
    private InterchangeValue<LocalTime> timeValue = InterchangeValue.unknown();


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

    /**
     * @return Id of observation in application.
     */
    @Override
    public String getId() {
        return flowsheetId;
    }

    /**
     * @return Most recent update to the observation type or observation
     */
    @Override
    public Instant getLastUpdatedInstant() {
        return updatedTime;
    }
}
