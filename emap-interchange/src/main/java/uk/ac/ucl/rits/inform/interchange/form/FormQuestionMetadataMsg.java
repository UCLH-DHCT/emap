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

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class FormQuestionMetadataMsg extends EmapOperationMessage implements Serializable  {
    // form question

    /**
     * The questions's name, eg. "ICU DISCUSSION"
     */
    private String name;

    /**
     * The questions's abbreviated name.
     */
    private String abbrevName;

    /**
     * The question's description.
     */
    private String description;

    /**
     * The instant that the form started existing in its present form.
     */
    private Instant validFrom;

    /**
     * How the source system describes the data type of an expected answer to this question.
     * Epic stores the possible values in the ZC_DATA_TYPE table.
     */
    private String internalDataType;

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
