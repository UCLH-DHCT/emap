package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Emap interchange message classes must implement this interface.
 *
 * @author Jeremy Stein
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class EmapOperationMessage implements Serializable {
    private String sourceMessageId;

    /**
     * Messages must call back out to the processor (double dispatch).
     * @param processor the Emap processor
     * @return the status code
     */
    public abstract String processMessage(EmapOperationMessageProcessor processor);

    /**
     * @return a short string describing the message type for human reading purposes (eg. logging).
     * you are encouraged to be more specific if your message has subtypes.
     */
    @JsonIgnore
    public String getMessageType() {
        return this.getClass().getName();
    }

    /**
     * A unique message ID that makes sense to the source system (eg, the IDS unid).
     * @return the message ID
     */
    public String getSourceMessageId() {
        return sourceMessageId;
    }

    /**
     * @param sourceMessageId set the source message Id from the source system
     */
    public void setSourceMessageId(String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }
}
