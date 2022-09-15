package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientConditionController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;


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
    private final VisitController visitController;

    /**
     * Patient state controller to identify whether state needs to be updated; person controller to identify patient.
     * @param patientConditionController patient state controller
     * @param personController           person controller
     * @param visitController            hospital visit controller
     */
    public PatientStateProcessor(
            PatientConditionController patientConditionController,
            PersonController personController, VisitController visitController) {
        this.patientConditionController = patientConditionController;
        this.personController = personController;
        this.visitController = visitController;
    }

    /**
     * Process patient condition (problem/infection/allergy) message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(PatientConditionMessage msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        logger.trace("Processing {}", msg);
        Mrn mrn = getOrCreateMrn(msg, storedFrom);
        HospitalVisit visit = getOrCreateHospitalVisit(msg, mrn, storedFrom);
        patientConditionController.processMessage(msg, mrn, visit, storedFrom);
    }

    /**
     * Get or create a hospital visit using the visitController.
     * @param msg          Patient condition message
     * @param mrn          MRN
     * @param storedFrom   Instant at which the message started being processed
     * @return HospitalVisit
     * @throws RequiredDataMissingException
     */
    private HospitalVisit getOrCreateHospitalVisit(PatientConditionMessage msg, Mrn mrn, Instant storedFrom)
            throws RequiredDataMissingException {

        HospitalVisit visit = null;

        if (msg.getVisitNumber().isSave()) {
            visit = visitController.getOrCreateMinimalHospitalVisit(msg.getVisitNumber().get(), mrn,
                    msg.getSourceSystem(), msg.getUpdatedDateTime(), storedFrom);
        }

        return visit;
    }

    /**
     * Get or create an MRN id associated with a patient.
     * @param msg          Patient condition message
     * @param storedFrom   Instant at which the message started being processed
     * @return HospitalVisit
     * @throws RequiredDataMissingException
     */
    private Mrn getOrCreateMrn(PatientConditionMessage msg, Instant storedFrom) throws RequiredDataMissingException {

        String mrnStr = msg.getMrn();
        Instant msgUpdatedTime = msg.getUpdatedDateTime();

        return personController.getOrCreateOnMrnOnly(mrnStr, null, msg.getSourceSystem(),
                msgUpdatedTime, storedFrom);
    }

}
