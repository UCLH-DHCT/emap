package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;

import java.time.Instant;

/**
 * Handle processing of ADT messages.
 * @author Stef Piatek
 */
@Component
public class AdtProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonController personController;
    private final VisitController visitController;

    /**
     * @param personController person interactions.
     * @param visitController  encounter interactions.
     */
    public AdtProcessor(PersonController personController, VisitController visitController) {
        this.personController = personController;
        this.visitController = visitController;
    }


    /**
     * Default processing of an ADT message.
     * @param msg        ADT message
     * @param storedFrom time that emap-core started processing the message.
     * @return return Code
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public String processMessage(final AdtMessage msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        Instant messageDateTime = msg.getRecordedDateTime();
        Mrn mrn = processPersonLevel(msg, storedFrom, messageDateTime);

        // Patient merges have no encounter information, so skip
        if (!(msg instanceof MergePatient)) {
            HospitalVisit visit = visitController.updateOrCreateHospitalVisit(msg, msg.getVisitNumber(), storedFrom, mrn);
        }


        return returnCode;
    }

    /**
     * Process person level information, saving changes to database.
     * @param msg             adt message
     * @param storedFrom      time that emap-core started processing the message.
     * @param messageDateTime date time of the message
     * @return MRN
     * @throws MessageIgnoredException if message is not set up to be processed yet
     */
    @Transactional
    public Mrn processPersonLevel(AdtMessage msg, Instant storedFrom, Instant messageDateTime) throws MessageIgnoredException {
        Mrn mrn = personController.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), msg.getSourceSystem(), msg.getRecordedDateTime(), storedFrom);
        personController.updateOrCreateDemographic(mrn, msg, messageDateTime, storedFrom);

        if (msg instanceof MergePatient) {
            MergePatient mergePatient = (MergePatient) msg;
            personController.mergeMrns(mergePatient.getPreviousMrn(), mergePatient.getPreviousNhsNumber(),
                    mrn, mergePatient.getRecordedDateTime(), storedFrom);
        }
        return mrn;
    }

    @Transactional
    public String deletePersonInformation(DeletePersonInformation msg, Instant storedFrom) {
        String returnCode = "OK";
        Instant messageDateTime = msg.getRecordedDateTime();
        Mrn mrn = personController.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), msg.getSourceSystem(), messageDateTime, storedFrom);
        personController.deleteDemographic(mrn, messageDateTime, storedFrom);
        visitController.deleteOlderVisits(mrn, msg, messageDateTime);

        return returnCode;
    }

    @Transactional
    public String moveVisitInformation(MoveVisitInformation msg, Instant storedFrom) throws MessageIgnoredException {
        String returnCode = "OK";
        Instant messageDateTime = msg.getRecordedDateTime();
        Mrn previousMrn = personController.getOrCreateMrn(
                msg.getPreviousMrn(), msg.getPreviousNhsNumber(), msg.getSourceSystem(), messageDateTime, storedFrom);
        Mrn currentMrn = processPersonLevel(msg, storedFrom, messageDateTime);
        HospitalVisit visit = visitController.moveVisitInformation(msg, storedFrom, previousMrn, currentMrn);
        return returnCode;
    }

    public String changePatientIdentifiers(ChangePatientIdentifiers msg, Instant storedFrom) {
        String returnCode = "OK";
        Instant messageDateTime = msg.getRecordedDateTime();
        Mrn previousMrn = personController.updatePatientIdentifiersOrCreateMrn(msg, messageDateTime, storedFrom);
        return returnCode;
    }
}
