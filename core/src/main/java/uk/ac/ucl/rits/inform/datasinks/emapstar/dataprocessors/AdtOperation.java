package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonRepository;
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
public class AdtOperation {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonRepository personRepo;

    /**
     * @param personRepo person repository.
     */
    public AdtOperation(PersonRepository personRepo) {
        this.personRepo = personRepo;
    }


    /**
     * Default processing of an ADT message.
     * @param msg        ADT message
     * @param storedFrom time that emap-core started processing the message.
     * @return return Code
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    public String processMessage(AdtMessage msg, Instant storedFrom) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        String sourceSystem = "EPIC";
        Mrn mrn = personRepo.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), sourceSystem, msg.getRecordedDateTime(), storedFrom);
        personRepo.updateOrCreateDemographics(mrn.getMrnId(), msg, storedFrom);

        if (msg instanceof MergeById) {
            MergeById mergeById = (MergeById) msg;
            personRepo.mergeMrns(mergeById.getRetiredMrn(), mrn, mergeById.getRecordedDateTime(), storedFrom);
        }
        return returnCode;
    }
}
