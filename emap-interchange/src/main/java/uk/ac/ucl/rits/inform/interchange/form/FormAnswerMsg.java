package uk.ac.ucl.rits.inform.interchange.form;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * An instance of an answered question (Eg. SDE), which will always be nested in a Form.
 * Doesn't extend EmapOperationMessage to disallow the sending of bare objects of this class.
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FormAnswerMsg {
    // would normally inherit this field from EmapOperationMessage but this class isn't a message on its own
    private String sourceMessageId;

    private String epicElementId;

    private String elementName;

    private String elementValue;
}
