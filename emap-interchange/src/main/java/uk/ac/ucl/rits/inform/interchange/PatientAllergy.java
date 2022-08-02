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
 * @author Tom Young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientAllergy extends PatientConditionMessage implements Serializable {
    private String mrn;
    private InterchangeValue<String> visitNumber = InterchangeValue.unknown();


    /**
     * Reaction (aka. symptom) occurring when patient is exposed to allergen...
     */
    private List<String> reactions = new ArrayList<>();


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
     * Status of the allergy...
     */
    private InterchangeValue<String> status = InterchangeValue.unknown();

    /**
     * Action in relation to allergy information, e.g. whether add/update/delete...
     */
    private String action;
    /**
     * Reaction occurring when patient exposed to allergen...
     */
    private List<String> reactions = new ArrayList<>();


    /**
     * Adds a list of strings to the reactions.
     * @param allergyReactions Collection of strings, each representing an allergy reaction.
     */
    public void addAllReactions(Collection<String> allergyReactions) {
        reactions.addAll(allergyReactions);
    }

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
