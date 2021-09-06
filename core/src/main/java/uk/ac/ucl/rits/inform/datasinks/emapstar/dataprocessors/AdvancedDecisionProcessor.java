package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.AdvancedDecisionController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.AdvancedDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.time.Instant;

/**
 * Advanced decision message processor.
 * @author Anika Cawthorn
 */
@Component
public class AdvancedDecisionProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AdvancedDecisionController advancedDecisionController;
    private final PersonController personController;
    private final VisitController visitController;

    /**
     * Set controllers needed to process advanced decisions.
     * @param advancedDecisionController   advanced decision controller
     * @param personController             person controller to link consultation request to patient
     * @param visitController              visit controller to link consultation request to hospital visit
     */
    public AdvancedDecisionProcessor(
            AdvancedDecisionController advancedDecisionController, PersonController personController,
            VisitController visitController) {
        this.advancedDecisionController = advancedDecisionController;
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
    public void processMessage(final AdvancedDecisionMessage msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant msgStatusChangeTime = msg.getStatusChangeTime();

        // retrieve patient to whom message refers to; if MRN not registered, create new patient
        Mrn mrn = personController.getOrCreateOnMrnOnly(msg.getMrn(), null, msg.getSourceSystem(),
                msgStatusChangeTime, storedFrom);
        HospitalVisit visit = visitController.getOrCreateMinimalHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), msg.getStatusChangeTime(), storedFrom);
        advancedDecisionController.processMessage(msg, visit, mrn, storedFrom);
    }
}
