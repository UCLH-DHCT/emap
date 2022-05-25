package uk.ac.ucl.rits.inform.interchange.form;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FormMsg extends EmapOperationMessage implements Serializable {
    // Eg. Epic SmartForm ID
    private String sourceSystemFormId;

    private Instant formFilingDatetime;

    private String formFilingUserId;

    /**
     * Can't guarantee we are going to get this field!
     */
    private String visitNumber;

    private String mrn;

    /**
     * The SDEs in this instance of the smart form. Ordered if we know the order!
     */
    private List<FormAnswerMsg> formAnswerMsgs = new ArrayList<>();

    /**
     * Messages must call back out to the processor (double dispatch).
     *
     * @param processor the Emap processor
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
