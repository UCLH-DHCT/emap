package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Emap interchange message classes must implement this interface.
 * @author Jeremy Stein
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class EmapOperationMessage implements Serializable {
    private String sourceMessageId;
    /**
     * Application that the message is sent from.
     */
    private String sourceSystem;

    /**
     * Messages must call back out to the processor (double dispatch).
     * @param processor the Emap processor
     * @return the status code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    public abstract String processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException;

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

    /**
     * @return {@link EmapOperationMessage#sourceSystem}
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem {@link EmapOperationMessage#sourceSystem}
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmapOperationMessage that = (EmapOperationMessage) o;
        return sourceSystem.equals(that.sourceSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceSystem);
    }

    @Override
    public String toString() {
        return String.format("EmapOperationMessage{sourceMessageId='%s', sourceSystem='%s'}", sourceMessageId, sourceSystem);
    }
}
