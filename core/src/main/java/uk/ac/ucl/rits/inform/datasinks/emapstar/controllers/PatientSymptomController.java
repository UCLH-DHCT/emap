package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionSymptomAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionSymptomRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptomAudit;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptom;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;


import java.time.Instant;


/**
 * Parses patient symptoms arising from conditions found in interchange messages.
 * <p>
 * @author Tom Young
 */
@Component
public class PatientSymptomController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConditionSymptomRepository conditionSymptomRepo;
    private final ConditionSymptomAuditRepository conditionSymptomAuditRepo;

    /**
     * @param conditionSymptomRepo       autowired PatientSymptomRepository
     * @param conditionSymptomAuditRepo  autowired PatientSymptomAuditRepository
     */
    public PatientSymptomController(ConditionSymptomRepository conditionSymptomRepo,
                                    ConditionSymptomAuditRepository conditionSymptomAuditRepo) {
        this.conditionSymptomRepo = conditionSymptomRepo;
        this.conditionSymptomAuditRepo = conditionSymptomAuditRepo;
    }

    /**
     * Process a message containing symptoms of a condition.
     * @param msg Message to process
     * @param condition Patient condition entity
     * @param storedFrom Start time for processing
     * @throws EmapOperationMessageProcessingException If the message cannot be processed
     */
    @Transactional
    public void processMessage(PatientConditionMessage msg, PatientCondition condition, Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        if (msg instanceof PatientAllergy){
            processAllergyMessage((PatientAllergy) msg, condition, storedFrom);
        }
        else{
            throw new RequiredDataMissingException("Type of the message *"+msg.getClass()+"* could not "+
                    "be processed");
        }
    }

    /**
     * Process an allergy message which has reactions, treated as a symptom of a condition
     * @param msg Patient allergy message to process
     * @param condition Patient condition entity
     * @param storedFrom Start time for processing
     */
    private void processAllergyMessage(PatientAllergy msg, PatientCondition condition, Instant storedFrom){

        for (String reactionName : msg.getReactions()){

            var symptomState = getOrCreateConditionSymptomState(msg, reactionName, condition, storedFrom);

            if (symptomShouldBeUpdated(msg, symptomState)) {
                symptomState.saveEntityOrAuditLogIfRequired(conditionSymptomRepo, conditionSymptomAuditRepo);
            }
        }
    }

    /**
     * Get a patient condition row state if it exists in the database, otherwise create a minimal version
     * @param msg Patient allergy message
     * @param symptomName Name of the symptom
     * @param condition Patient condition entity
     * @param storedFrom Start time for processing
     * @return Condition rowstate
     */
    private RowState<ConditionSymptom, ConditionSymptomAudit> getOrCreateConditionSymptomState(
            PatientConditionMessage msg, String symptomName, PatientCondition condition, Instant storedFrom){

        var possibleSymptom = conditionSymptomRepo.findByNameAndPatientConditionId(symptomName, condition);
        Instant updatedTime = msg.getUpdatedDateTime();

        return possibleSymptom
                .map(obs -> new RowState<>(obs, updatedTime, storedFrom, false))
                .orElseGet(() -> new RowState<>(new ConditionSymptom(symptomName, condition), updatedTime, storedFrom, true));
    }

    /**
     * Should the symptom be updated in the database?
     * @param msg Patient condition message
     * @param symptom Patient symptom rowstate
     * @return True if there should be an update
     */
    private boolean symptomShouldBeUpdated(PatientConditionMessage msg,
                                           RowState<ConditionSymptom, ConditionSymptomAudit> symptom) {
        return symptom.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(symptom.getEntity().getPatientConditionId().getValidFrom());
    }
}
