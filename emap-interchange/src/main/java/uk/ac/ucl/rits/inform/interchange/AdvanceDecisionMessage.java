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
 * Advance decision of a patient, e.g. to not be resuscitated should the situation arise while the patient is in
 * hospital for treatment.
 * @author Anika Cawthorn
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class AdvanceDecisionMessage extends EmapOperationMessage implements Serializable {
    /**
     * Unique epic ID for advance decision.
     */
    private Long advanceDecisionNumber;

    /**
     * Patient ID this advance decision relates to.
     */
    private String mrn;

    /**
     * Hospital visit of the patient that this advance decision relates to.
     */
    private String visitNumber;

    /**
     * Advance decision type code used by EPIC.
     * e.g. COD4
     */
    private String advanceCareCode;

    /**
     * Advance decision type name used by EPIC.
     * e.g. DNACPR
     */
    private String advanceDecisionTypeName;

    /**
     * Questions and answers for advance decisions.
     */
    private Map<String, String> questions = new HashMap<>();

    /**
     * Last updated time.
     */
    private Instant statusChangeDatetime;

    /**
     * Time advance decision was first recorded for patient and hospital visit.
     */
    private Instant requestedDatetime;

    /**
     * Indicates whether the advance decision order has been cancelled by the user.
     */
    private boolean cancelled = false;

    /**
     * Indicates whether the advance decision order has been closed due to the patient no longer being in hospital.
     */
    private boolean closedDueToDischarge = false;

    /**
     * Constructor to set source ID and system and patient and hospital visit identifying information.
     * @param sourceId      Identifier assigned to message so that it can be retrieved from IDS.
     * @param sourceSystem  From which system this message was retrieved (e.g. HL7 stream or existing database).
     * @param mrn           Patient ID this advance decision relates to.
     * @param visitNumber   Hospital visit of patient this advance decision relates to.
     */
    public AdvanceDecisionMessage(String sourceId, String sourceSystem, String mrn, String visitNumber) {
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
