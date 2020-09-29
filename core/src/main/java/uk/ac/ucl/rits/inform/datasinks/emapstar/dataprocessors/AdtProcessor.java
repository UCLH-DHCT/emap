package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
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

    /**
     * @param personController person data.
     */
    public AdtProcessor(PersonController personController) {
        this.personController = personController;
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
        Mrn mrn = personController.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), msg.getSourceSystem(), msg.getRecordedDateTime(), storedFrom);
        personController.updateOrCreateDemographic(mrn, msg, msg.getRecordedDateTime(), storedFrom);

        if (msg instanceof MergePatient) {
            MergePatient mergePatient = (MergePatient) msg;
            personController.mergeMrns(mergePatient.getRetiredMrn(), mergePatient.getRetiredNhsNumber(),
                    mrn, mergePatient.getRecordedDateTime(), storedFrom);
        }
        return returnCode;
    }
}
