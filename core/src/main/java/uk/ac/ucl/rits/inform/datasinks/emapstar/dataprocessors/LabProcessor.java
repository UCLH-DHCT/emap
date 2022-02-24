package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.ClimbSequenceController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.LabController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.ClimbSequenceMsg;
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
    private final ClimbSequenceController climbSequenceController;

    /**
     * @param personController        controller for person tables
     * @param visitController         controller for visit tables
     * @param labController           controller for lab tables
     * @param climbSequenceController controls interactions with climb sequence tables
     */
    public LabProcessor(PersonController personController, VisitController visitController,
                        LabController labController, ClimbSequenceController climbSequenceController) {
        this.personController = personController;
        this.visitController = visitController;
        this.labController = labController;
        this.climbSequenceController = climbSequenceController;
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
        Instant collectionDateTime = msg.getCollectionDateTime();
        Mrn mrn = personController.getOrCreateMrn(mrnStr, null, msg.getSourceSystem(), collectionDateTime, storedFrom);
        HospitalVisit visit = null;
        try {
            visit = visitController.getOrCreateMinimalHospitalVisit(
                    msg.getVisitNumber(), mrn, msg.getSourceSystem(), collectionDateTime, storedFrom);
        } catch (RequiredDataMissingException e) {
            logger.debug("No visit for LabOrder, skipping creating an encounter");
        }
        labController.processLabOrder(mrn, visit, msg, storedFrom);
    }

    /**
     * Process a MRC CLIMB sequence message.
     * @param msg        climb sequence
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException
     */
    @Transactional
    public void processMessage(final ClimbSequenceMsg msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        LabSample labSample = null;
        if (msg.getSpecimenBarcode() != null) {
            labSample = labController.getLabSampleOrThrow(msg.getSpecimenBarcode());
        }
        climbSequenceController.processSequence(msg, labSample, msg.getSequenceValidFrom(), storedFrom);
    }
}
