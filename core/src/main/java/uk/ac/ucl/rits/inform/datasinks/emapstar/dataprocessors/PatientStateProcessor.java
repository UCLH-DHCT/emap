package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientStateController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;
import java.time.LocalDate;


/**
 * Handle processing of patient state messages.
 * @author Anika Cawthorn
 */
@Component
public class PatientStateProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientStateController patientStateController;
    private final PersonController personController;
    private final VisitController visitController;

    /**
     * @param patientStateController     patient state controller
     * @param personController           person controller
     * @param visitController            visit controller
     */
    public PatientStateProcessor(
            PatientStateController patientStateController, PersonController personController,
            VisitController visitController) {
        this.patientStateController = patientStateController;
        this.personController = personController;
        this.visitController = visitController;
    }

    /**
     * Process flowsheet message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final PatientInfection msg, @Nullable HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getUpdatedDateTime();

        Mrn mrn = personController.getOrCreateMrn(mrnStr, null, msg.getSourceSystem(), observationTime, storedFrom);
//        HospitalVisit visit = visitController.getOrCreateMinimalHospitalVisit(
//                msg., mrn, msg.getSourceSystem(), observationTime, storedFrom);
    }
}
