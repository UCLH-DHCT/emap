package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Emap interchange message classes must implement this interface.
 *
 * @author Jeremy Stein
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface EmapOperationMessage extends Serializable {
    /**
     * Messages must call back out to the processor (double dispatch).
     * @param processor the Emap processor
     */
    void processMessage(EmapOperationMessageProcessor processor);
}
