package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.ConsultationRequestController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;

import java.time.Instant;

/**
 * Consultation request message processor.
 * @author Anika Cawthorn
 */
@Component
public class ConsultationRequestProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConsultationRequestController consultationRequestController;
    private final PersonController personController;
    private final VisitController visitController;

    /**
     * Patient state controller to identify whether state needs to be updated; person controller to identify patient.
     * @param consultationRequestController   consultation request controller
     * @param personController           person controller so that consultation request can be linked to patient
     * @param visitController            visit controller so that consultation request can be linked to hospital visit
     */
    public ConsultationRequestProcessor(
            ConsultationRequestController consultationRequestController, PersonController personController,
            VisitController visitController) {
        this.consultationRequestController = consultationRequestController;
        this.personController = personController;
        this.visitController = visitController;
    }

    /**
     * Process patient infection message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final ConsultRequest msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant msgStatusChangeTime = msg.getStatusChangeTime();

        // retrieve patient to whom message refers to; if MRN not registered, create new patient
        Mrn mrn = personController.getOrCreateOnMrnOnly(mrnStr, null, msg.getSourceSystem(),
                msgStatusChangeTime, storedFrom);
        HospitalVisit visit = visitController.getOrCreateMinimalHospitalVisit(
                    msg.getVisitNumber(), mrn, msg.getSourceSystem(), msg.getStatusChangeTime(), storedFrom);
        consultationRequestController.processMessage(msg, visit, storedFrom);
    }
}
