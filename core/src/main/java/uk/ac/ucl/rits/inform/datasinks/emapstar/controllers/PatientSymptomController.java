package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientSymptomAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientSymptomRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
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
    private final PatientSymptomRepository patientSymptomRepo;
    private final PatientSymptomAuditRepository patientSymptomAuditRepo;


    /**
     * @param patientSymptomRepo      autowired PatientSymptomRepository
     * @param patientSymptomAuditRepo autowired PatientSymptomAuditRepository
     */
    public PatientSymptomController(PatientSymptomRepository patientSymptomRepo,
                                    PatientSymptomAuditRepository patientSymptomAuditRepo) {
        this.patientSymptomRepo = patientSymptomRepo;
        this.patientSymptomAuditRepo = patientSymptomAuditRepo;
    }


    @Transactional
    public void processMessage(PatientConditionMessage msg, Mrn mrn, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        if (msg.getClass() == PatientAllergy.class){
            // Do things
        }

        // No other message types support symptoms
    }



}
