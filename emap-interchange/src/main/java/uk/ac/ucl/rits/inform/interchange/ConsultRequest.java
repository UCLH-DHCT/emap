package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Requests for specialist consultations for a patient.
 * @author Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class ConsultRequest extends EmapOperationMessage implements Serializable {
    /**
     * Unique epic ID for for the consultation.
     */
    private Long epicConsultId;

    private String mrn;
    private String visitNumber;

    /**
     * Consultation code used by EPIC.
     * e.g. CON123
     */
    private String consultationType;

    /**
     * Last updated time.
     */
    private Instant statusChangeTime;

    /**
     * Time the consult was requested.
     */
    private Instant requestedDateTime;

    /**
     * Has the request for a consult been cancelled.
     */
    private boolean isCancelled = false;

    /**
     * Questions and answers for consult questions.
     */
    private Map<String, String> questions = new HashMap<>();
    /**
     * Notes or further information about the reason for the request.
     */
    private InterchangeValue<String> notes = InterchangeValue.unknown();

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor)
            throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}