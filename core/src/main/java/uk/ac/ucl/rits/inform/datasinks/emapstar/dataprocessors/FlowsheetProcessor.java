package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitObservationController;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;

import java.time.Instant;

/**
 * Handle processing of Flowsheet messages.
 * @author Stef Piatek
 */
@Component
public class FlowsheetProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonController personController;
    private final VisitController visitController;
    private final VisitObservationController visitObservationController;

    /**
     * @param personController           person controller.
     * @param visitController            visit controller
     * @param visitObservationController visit observation controller
     */
    public FlowsheetProcessor(PersonController personController, VisitController visitController, VisitObservationController visitObservationController) {
        this.personController = personController;
        this.visitController = visitController;
        this.visitObservationController = visitObservationController;
    }

    /**
     * Process flowsheet message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final Flowsheet msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getObservationTime();
        Mrn mrn = personController.getOrCreateMrn(mrnStr, null, msg.getSourceSystem(), observationTime, storedFrom);
        HospitalVisit visit = visitController.getOrCreateMinimalHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), observationTime, storedFrom);
        visitObservationController.processFlowsheet(msg, visit, storedFrom);
    }
}
