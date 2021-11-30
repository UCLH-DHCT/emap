package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class ConsultMetadata extends EmapOperationMessage {
    private static final long serialVersionUID = -4230541136107660086L;

    private String code;
    private String name;
    private Instant lastUpdatedDate;

    /**
     * Create fully-populated ConsultMetadata message.
     * @param code            code for the consult type
     * @param name            human readable name for the consult type
     * @param lastUpdatedDate creation or update time of the consult type
     * @param sourceSystem    system that the information has come from
     */
    public ConsultMetadata(String code, String name, Instant lastUpdatedDate, String sourceSystem) {
        setSourceSystem(sourceSystem);
        setSourceMessageId(code);
        this.code = code;
        this.name = name;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    /**
     * Messages must call back out to the processor (double dispatch).
     * @param processor the Emap processor
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
