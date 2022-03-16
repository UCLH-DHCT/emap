package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
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

    /**
     * Reaction (aka. symptom) occurring when patient is exposed to allergen...
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
        reactions.addAll(allergyReactions);
    }

    /**
     * Type of allergy, i.e. whether patient is allergic against a drug, particles in the environment, etc.
     */
    InterchangeValue<String> getAllergyType(){ return getSubType(); }
}
