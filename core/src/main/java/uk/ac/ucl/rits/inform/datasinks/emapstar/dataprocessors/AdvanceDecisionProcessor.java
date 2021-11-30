package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.AdvanceDecisionController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.AdvanceDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.time.Instant;

/**
 * Advance decision message processor that retrieves or creates all entities required to update or create an advance
 * care decision.
 * @author Anika Cawthorn
 */
@Component
public class AdvanceDecisionProcessor {
    private final AdvanceDecisionController advanceDecisionController;
    private final PersonController personController;
    private final VisitController visitController;

    /**
     * Set controllers needed to process advance decisions.
     * @param advanceDecisionController   advance decision controller
     * @param personController             person controller to link advance decision to patient
     * @param visitController              visit controller to link advance decision to hospital visit
     */
    public AdvanceDecisionProcessor(
            AdvanceDecisionController advanceDecisionController, PersonController personController,
            VisitController visitController) {
        this.advanceDecisionController = advanceDecisionController;
        this.personController = personController;
        this.visitController = visitController;
    }

    /**
     * Process advance decision message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final AdvanceDecisionMessage msg, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant msgStatusChangeTime = msg.getStatusChangeDatetime();

        // retrieve patient to whom message refers to; if MRN not registered, create new patient
        Mrn mrn = personController.getOrCreateOnMrnOnly(msg.getMrn(), null, msg.getSourceSystem(),
                msgStatusChangeTime, storedFrom);
        HospitalVisit visit = visitController.getOrCreateMinimalHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), msg.getStatusChangeDatetime(), storedFrom);
        advanceDecisionController.processMessage(msg, visit, mrn, storedFrom);
    }
}
