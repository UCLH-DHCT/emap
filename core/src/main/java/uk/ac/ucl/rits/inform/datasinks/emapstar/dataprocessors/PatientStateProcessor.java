package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientConditionController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;

/**
 * Handle processing of patient state messages.
 * @author Anika Cawthorn
 */
@Component
public class PatientStateProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientConditionController patientConditionController;
    private final PersonController personController;

    /**
     * Patient state controller to identify whether state needs to be updated; person controller to identify patient.
     * @param patientConditionController     patient state controller
     * @param personController           person controller
     */
    public PatientStateProcessor(
            PatientConditionController patientConditionController, PersonController personController) {
        this.patientConditionController = patientConditionController;
        this.personController = personController;
    }

    /**
     * Process patient infection message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final PatientInfection msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant msgUpdatedTime = msg.getUpdatedDateTime();

        // retrieve patient to whom message refers to; if MRN not registered, create new patient
        Mrn mrn = personController.getOrCreateOnMrnOnly(mrnStr, null, msg.getSourceSystem(),
                msgUpdatedTime, storedFrom);

        patientConditionController.processMessage(msg, mrn, storedFrom);
    }


}
