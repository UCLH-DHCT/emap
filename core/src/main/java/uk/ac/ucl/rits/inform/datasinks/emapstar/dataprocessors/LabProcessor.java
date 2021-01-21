package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.LabController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;

/**
 * Handle processing of Lab messages.
 * @author Stef Piatek
 */
@Component
public class LabProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonController personController;
    private final VisitController visitController;
    private final LabController labController;

    public LabProcessor(PersonController personController, VisitController visitController, LabController labController) {
        this.personController = personController;
        this.visitController = visitController;
        this.labController = labController;
    }

    /**
     * Process Lab message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final LabOrderMsg msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getObservationDateTime();
        Mrn mrn = personController.getOrCreateMrn(mrnStr, null, msg.getSourceSystem(), observationTime, storedFrom);
        HospitalVisit visit = null;
        try {
            visit = visitController.getOrCreateMinimalHospitalVisit(
                    msg.getVisitNumber(), mrn, msg.getSourceSystem(), observationTime, storedFrom);
        } catch (RequiredDataMissingException e) {
            logger.debug("No visit for LabOrder, skipping creating an encounter");
        }
        labController.processLabOrder(mrn, visit, msg, storedFrom);
    }
}
