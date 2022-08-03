package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AllergenReactionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.AllergenReaction;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;


/**
 * Parses patient symptoms arising from conditions found in interchange messages.
 * <p>
 * @author Tom Young
 */
@Component
public class PatientSymptomController {

    private final AllergenReactionRepository conditionSymptomRepo;

    /**
     * @param conditionSymptomRepo       autowired PatientSymptomRepository
     */
    public PatientSymptomController(AllergenReactionRepository conditionSymptomRepo) {
        this.conditionSymptomRepo = conditionSymptomRepo;
    }

    /**
     * Process a message containing symptoms of a condition.
     * @param msg Message to process
     * @param condition Patient condition entity
     * @throws EmapOperationMessageProcessingException If the message cannot be processed
     */
    @Transactional
    public void processMessage(PatientConditionMessage msg, PatientCondition condition)
            throws EmapOperationMessageProcessingException {

        if (msg instanceof PatientAllergy){
            processAllergyMessage((PatientAllergy) msg, condition);
        }

        // No other messages yet have symptoms
    }

    /**
     * Process an allergy message which has reactions, treated as a symptom of a condition
     * @param msg Patient allergy message to process
     * @param condition Patient condition entity
     */
    private void processAllergyMessage(PatientAllergy msg, PatientCondition condition){

        for (String reactionName : msg.getReactions()){

            var symptom = new AllergenReaction(reactionName, condition);
            if (conditionSymptomRepo.findByNameAndPatientConditionId(reactionName, condition).isEmpty()){
                conditionSymptomRepo.save(symptom);
            }
        }
    }

}
