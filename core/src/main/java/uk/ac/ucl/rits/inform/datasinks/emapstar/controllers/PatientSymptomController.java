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
import java.util.Optional;


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
     * @param conditionSymptomRepo      autowired PatientSymptomRepository
     */
    public PatientSymptomController(ConditionSymptomRepository conditionSymptomRepo,
                                    ConditionSymptomAuditRepository conditionSymptomAuditRepo) {
        this.conditionSymptomRepo = conditionSymptomRepo;
        this.conditionSymptomAuditRepo = conditionSymptomAuditRepo;
    }

    /**
     * @param msg
     * @param condition
     * @param storedFrom
     * @throws EmapOperationMessageProcessingException
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
     * @param msg
     * @param condition
     * @param storedFrom
     */
    private void processAllergyMessage(PatientAllergy msg, PatientCondition condition, Instant storedFrom){

        for (String reactionName : msg.getReactions()){

            RowState<ConditionSymptom, ConditionSymptomAudit> symptomState = getOrCreateConditionSymptomState(msg,
                    reactionName, condition, storedFrom);

            if (!symptomShouldBeUpdated(msg, symptomState)) {
                continue;
            }

            symptomState.saveEntityOrAuditLogIfRequired(conditionSymptomRepo, conditionSymptomAuditRepo);
        }
    }

    /**
     * @param msg
     * @param symptomName
     * @param condition
     * @param storedFrom
     * @return
     */
    private RowState<ConditionSymptom, ConditionSymptomAudit> getOrCreateConditionSymptomState(
            PatientConditionMessage msg, String symptomName, PatientCondition condition, Instant storedFrom){

        Optional<ConditionSymptom> possibleSymptom = conditionSymptomRepo.findByNameAndPatientConditionId(symptomName,
                condition);
        Instant updatedTime = msg.getUpdatedDateTime();

        return possibleSymptom
                .map(obs -> new RowState<>(obs, updatedTime, storedFrom, false))
                .orElseGet(() -> createMinimalConditionSymptom(symptomName, condition, updatedTime, storedFrom));
    }

    /**
     * @param msg
     * @param symptom
     * @return
     */
    private boolean symptomShouldBeUpdated(PatientConditionMessage msg,
                                           RowState<ConditionSymptom, ConditionSymptomAudit> symptom) {
        return symptom.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(symptom.getEntity().getPatientConditionId().getValidFrom());
    }

    /**
     * @param symptomName
     * @param condition
     * @param validFrom
     * @param storedFrom
     * @return
     */
    private RowState<ConditionSymptom, ConditionSymptomAudit> createMinimalConditionSymptom(String symptomName,
                            PatientCondition condition,Instant validFrom, Instant storedFrom){

        ConditionSymptom symptom = new ConditionSymptom(symptomName, condition);
        return new RowState<>(symptom, validFrom, storedFrom, true);
    }

}
