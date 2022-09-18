package uk.ac.ucl.rits.inform.interchange.form;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.time.Instant;
import java.time.LocalDate;

/**
 * An instance of an answered question (Eg. SDE), which will always be nested in a Form.
 * Doesn't extend EmapOperationMessage to disallow the sending of bare objects of this class.
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FormAnswerMsg {
    // would normally inherit this field from EmapOperationMessage but this class isn't a message on its own
    private String sourceMessageId;

    /**
     * The question that this answer is answering.
     */
    private String questionId;

    /**
     * "NOTE", "ORDER", etc.
     */
    private String context;

    /**
     * If the form answer has been updated since the form was first filed, then
     * the filing datetime can be overridden here.
     */
    private Instant validFrom;

    // These different values depend on knowing the type in the SDE data itself,
    // as a datasource won't have access to the metadata!
    // Until that's available we will have to use the string value only.

    /**
     * Numeric value.
     */
    private InterchangeValue<Double> numericValue = InterchangeValue.unknown();

    /**
     * String value.
     */
    private InterchangeValue<String> stringValue = InterchangeValue.unknown();

    /**
     * UTC timestamp value.
     */
    private InterchangeValue<Instant> utcDatetimeValue = InterchangeValue.unknown();

    /**
     * Date value.
     */
    private InterchangeValue<LocalDate> dateValue = InterchangeValue.unknown();

    /**
     * Boolean value.
     */
    private InterchangeValue<Boolean> booleanValue = InterchangeValue.unknown();
}
