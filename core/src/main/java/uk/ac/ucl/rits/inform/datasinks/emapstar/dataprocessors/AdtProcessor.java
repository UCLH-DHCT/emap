package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.EncounterController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;

import java.time.Instant;

/**
 * Handle processing of ADT messages.
 * @author Stef Piatek
 */
@Component
public class AdtProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonController personController;
    private final EncounterController encounterController;

    /**
     * @param personController    person interactions.
     * @param encounterController encounter interactions.
     */
    public AdtProcessor(PersonController personController, EncounterController encounterController) {
        this.personController = personController;
        this.encounterController = encounterController;
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
        Mrn mrn = personController.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), msg.getSourceSystem(), msg.getRecordedDateTime(), storedFrom);
        personController.updateOrCreateDemographic(mrn, msg, messageDateTime, storedFrom);

        if (msg instanceof MergePatient) {
            MergePatient mergePatient = (MergePatient) msg;
            personController.mergeMrns(mergePatient.getRetiredMrn(), mergePatient.getRetiredNhsNumber(),
                    mrn, mergePatient.getRecordedDateTime(), storedFrom);
        }
        HospitalVisit visit = encounterController.getOrCreateHospitalVisit(
                msg.getVisitNumber(), mrn, msg.getSourceSystem(), messageDateTime, storedFrom, true);
        AuditHospitalVisit auditHospitalVisit = encounterController.updateVisitIfUntrustedSystemOrNewlyCreated(
                msg, messageDateTime, storedFrom, visit);

        return returnCode;
    }
}
