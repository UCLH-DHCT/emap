package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonData;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

import java.time.Instant;

/**
 * Handle processing of ADT messages.
 * @author Stef Piatek
 */
@Component
public class AdtProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonData personData;

    /**
     * @param personData person data.
     */
    public AdtProcessor(PersonData personData) {
        this.personData = personData;
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
        String sourceSystem = "EPIC";
        Mrn mrn = personData.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), sourceSystem, msg.getRecordedDateTime(), storedFrom);
        personData.updateOrCreateDemographic(mrn.getMrnId(), msg, storedFrom);

        if (msg instanceof MergeById) {
            MergeById mergeById = (MergeById) msg;
            personData.mergeMrns(mergeById.getRetiredMrn(), mrn, mergeById.getRecordedDateTime(), storedFrom);
        }
        return returnCode;
    }
}
