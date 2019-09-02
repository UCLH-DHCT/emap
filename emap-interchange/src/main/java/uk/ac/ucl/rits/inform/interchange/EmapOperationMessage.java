package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Emap interchange message classes must implement this interface.
 * @author jeremystein
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface EmapOperationMessage extends Serializable {
    public void processMessage(EmapOperationMessageProcessor processor);
}
