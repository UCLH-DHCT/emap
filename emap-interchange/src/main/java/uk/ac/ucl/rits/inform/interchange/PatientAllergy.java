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
    public InterchangeValue<String> getAllergenType(){ return getSubType(); }

    public void setAllergenType(InterchangeValue<String> value){ setSubType(value); }
    public void setAllergenType(String value){ setSubType(new InterchangeValue<>(value)); }

    public InterchangeValue<String> getAllergenName(){ return getConditionName(); }
    public void setAllergenName(InterchangeValue<String> AllergyName){ setConditionName(AllergyName);}

    public InterchangeValue<Long> getEpicAllergyId(){ return getEpicConditionId(); }
    public void setEpicAllergyId(InterchangeValue<Long> epicAllergyId){ setEpicConditionId(epicAllergyId); }

    public InterchangeValue<Instant> getAllergyResolved(){ return getResolvedTime(); }
    public void setAllergyResolved(InterchangeValue<Instant> resolvedTime){ setResolvedTime(resolvedTime); }

    public Instant getAllergyAdded(){ return getAddedTime(); }
    public void setAllergyAdded(Instant addedTime){ setAddedTime(addedTime); }

    public InterchangeValue<LocalDate> getAllergyOnset(){ return getOnsetTime(); }
    public void setAllergyOnset(InterchangeValue<LocalDate> date){setOnsetTime(date);}
}
