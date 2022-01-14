package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Interchange format of a PatientAllergy message.
 * @author Anika Cawthorn
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientAllergy extends EmapOperationMessage implements Serializable {
    private String mrn;
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();

    /**
     * Type of allergy, i.e. whether patient is allergic against a drug, particles in the environment, etc.
     */
    private String allergenType;

    /**
     * Human readable allergen name.
     */
    private InterchangeValue<String> allergenName = InterchangeValue.unknown();

    /**
     * Time of the update or message carrying this information.
     */
    private Instant updatedDateTime;

    /**
     * Unique Id for allergy in EPIC.
     */
    private InterchangeValue<Long> epicAllergyId = InterchangeValue.unknown();

    /**
     * Status of infection.
     */
    private InterchangeValue<String> status = InterchangeValue.unknown();

    /**
     * Infection added at...
     */
    private Instant allergyAdded;

    /**
     * Infection resolved at...
     */
    private InterchangeValue<Instant> allergyResolved = InterchangeValue.unknown();

    /**
     * Onset of infection known at...
     */
    private InterchangeValue<LocalDate> allergyOnset = InterchangeValue.unknown();

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
