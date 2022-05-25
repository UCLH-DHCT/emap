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

    private String epicElementId;

    private String elementName;

    private String elementValue;
}
