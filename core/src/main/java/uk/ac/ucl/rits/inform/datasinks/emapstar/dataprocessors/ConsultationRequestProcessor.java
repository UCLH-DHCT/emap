package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.ConsultationRequestController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.ConsultMetadata;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

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
     * Set controllers needed to process consultation requests.
     * @param consultationRequestController consultation request controller
     * @param personController              person controller to link consultation request to patient
     * @param visitController               visit controller to link consultation request to hospital visit
     */
    public ConsultationRequestProcessor(
            ConsultationRequestController consultationRequestController, PersonController personController,
            VisitController visitController) {
        this.consultationRequestController = consultationRequestController;
        this.personController = personController;
        this.visitController = visitController;
    }

    /**
     * Process consultation request message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final ConsultRequest msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant msgStatusChangeTime = msg.getStatusChangeDatetime();

        // retrieve patient to whom message refers to; if MRN not registered, create new patient
        Mrn mrn = personController.getOrCreateOnMrnOnly(msg.getMrn(), null, msg.getSourceSystem(),
                msgStatusChangeTime, storedFrom);
        HospitalVisit visit = visitController.getOrCreateMinimalHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), msg.getStatusChangeDatetime(), storedFrom);
        consultationRequestController.processMessage(msg, visit, storedFrom);
    }

    /**
     * Process consultation request metadata message.
     * @param msg        message
     * @param storedFrom time that the message was started to be processed by star
     */
    @Transactional
    public void processMessage(final ConsultMetadata msg, final Instant storedFrom) {
        consultationRequestController.processMessage(msg, storedFrom);
    }
}
