package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * Time of the update for the message carrying this information.
     */
    private Instant updatedDateTime;

    /**
     * Unique Id for allergy in EPIC.
     */
    private InterchangeValue<Long> epicAllergyId = InterchangeValue.unknown();

    /**
     * Allergy added at...
     */
    private Instant allergyAdded;

    /**
     * Onset of allergy known at...
     */
    private InterchangeValue<LocalDate> allergyOnset = InterchangeValue.unknown();

    /**
     * Severity of reaction patient shows when exposed to allergen...
     */
    private String severity;
    /**
     * Reaction occurring when patient exposed to allergen...
     */
    private List<String> reactions = new ArrayList<>();
    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    /**
     * Adds a list of strings to the reactions.
     * @param allergyReactions Collection of strings, each representing an allergy reaction.
     */
    public void addAllReactions(Collection<String> allergyReactions) {
        for (String reaction : allergyReactions) {
            reactions.add(reaction);
        }
    }
}
