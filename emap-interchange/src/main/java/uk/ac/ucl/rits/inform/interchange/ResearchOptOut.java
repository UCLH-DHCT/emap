package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@AllArgsConstructor
@NoArgsConstructor
public class ResearchOptOut extends EmapOperationMessage {
    /**
     * Nhs number, can be null if MRN is not null.
     */
    private String nhsNumber;
    /**
     * Master record number for hospital, can be null if MRN is not null.
     */
    private String mrn;
    /**
     * The datetime that the patient was last updated in EPIC, defaulting to the first day of 2019.
     */
    private Instant lastUpdated;

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
