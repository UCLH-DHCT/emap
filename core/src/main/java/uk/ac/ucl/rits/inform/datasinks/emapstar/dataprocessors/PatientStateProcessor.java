package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientConditionController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientSymptomController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptom;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;

import java.time.Instant;
import java.util.List;

/**
 * Handle processing of patient state messages.
 * @author Anika Cawthorn
 */
@Component
public class PatientStateProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientConditionController patientConditionController;
    private final PatientSymptomController patientSymptomController;
    private final PersonController personController;
    private final VisitController visitController;

    /**
     * Patient state controller to identify whether state needs to be updated; person controller to identify patient.
     * @param patientConditionController     patient state controller
     * @param personController               person controller
     * @param visitController                hospital visit controller
     */
    public PatientStateProcessor(
            PatientConditionController patientConditionController, PersonController personController,
            VisitController visitController, PatientSymptomController symptomController) {
        this.patientConditionController = patientConditionController;
        this.patientSymptomController = symptomController;
        this.personController = personController;
        this.visitController = visitController;
    }

    /**
     * Process patient condition message, which can represent either an infection, problem or allergy of a patient.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(PatientConditionMessage msg, final Instant storedFrom)
        throws EmapOperationMessageProcessingException {

        String mrnStr = msg.getMrn();
        Instant msgUpdatedTime = msg.getUpdatedDateTime();
        Mrn mrn = personController.getOrCreateOnMrnOnly(mrnStr, null, msg.getSourceSystem(),
                msgUpdatedTime, storedFrom);

        HospitalVisit visit = null;

        if (msg.getVisitNumber().isSave()){
            visit = visitController.getOrCreateMinimalHospitalVisit(msg.getVisitNumber().get(), mrn,
                    msg.getSourceSystem(), msgUpdatedTime, storedFrom);
        }
        PatientCondition condition = patientConditionController.getOrCreatePatientCondition(msg, mrn, visit, storedFrom);

        if (condition != null){
            patientSymptomController.processMessage(msg, condition, storedFrom);
        }
    }
}
