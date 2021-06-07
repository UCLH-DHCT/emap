package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Requests for specialist consultation for a patient.
 * @author Stef Piatek
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
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
     * Questions and answers for consult questions.
     */
    private Map<String, String> questions = new HashMap<>();
    /**
     * Notes or further information about the reason for the request.
     */
    private InterchangeValue<String> notes = InterchangeValue.unknown();

    /**
     * Has the request for a consult been cancelled.
     */
    private boolean cancelled = false;

    /**
     * Has the request been closed because of discharing the patient.
     */
    private boolean closedDueToDischarge = false;

    public ConsultRequest(String sourceId, String sourceSystem, String mrn, String visitNumber) {
        setSourceMessageId(sourceId);
        setSourceSystem(sourceSystem);
        this.mrn = mrn;
        this.visitNumber = visitNumber;
    }


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
