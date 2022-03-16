package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientSymptomAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientSymptomRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptom;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptomAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Parses patient symptoms arising from conditions found in interchange messages.
 * <p>
 * @author Tom Young
 */
@Component
public class PatientSymptomController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientSymptomRepository patientSymptomRepo;


    /**
     * @param patientSymptomRepo      autowired PatientSymptomRepository
     */
    public PatientSymptomController(PatientSymptomRepository patientSymptomRepo) {
        this.patientSymptomRepo = patientSymptomRepo;
    }


    @Transactional
    public List<ConditionSymptom> getOrCreateSymptoms(PatientConditionMessage msg)
            throws EmapOperationMessageProcessingException {

        if (msg.getClass() == PatientAllergy.class){
            return getOrCreateAllergyReactions((PatientAllergy) msg);
        }

        throw new RequiredDataMissingException("Could not process a "+msg.getClass()+" message. Not a supported type");
    }


    /**
     * Create symptoms for an allergic reaction
     * @param msg          Allergy message
     */
    private List<ConditionSymptom> getOrCreateAllergyReactions(PatientAllergy msg){

        List<ConditionSymptom> reactions = new ArrayList<>();

        for (String reactionName : msg.getReactions()){

            Optional<ConditionSymptom> symptom = patientSymptomRepo.findByName(reactionName);

            if (symptom.isPresent() && reactions.contains(symptom.get())){
                logger.debug("Reaction already present in the list of reactions. Not including");
                continue;
            }

            reactions.add(symptom
                    .orElseGet(() -> new ConditionSymptom(reactionName)));
        }

        return reactions;
    }

}
