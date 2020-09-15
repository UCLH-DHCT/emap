package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.time.Instant;

/**
 * Handle processing of VitalSigns messages.
 * @author Stef Piatek
 */
@Component
public class FlowsheetProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonRepository personRepo;

    /**
     * @param personRepo person repository.
     */
    public FlowsheetProcessor(PersonRepository personRepo) {
        this.personRepo = personRepo;
    }

    /**
     * Process vitalsigns message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @return return code
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    public String processMessage(VitalSigns msg, Instant storedFrom) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getObservationTimeTaken();
        String sourceSystem = msg.getVitalSignIdentifier().split("\\$")[0];
        Mrn mrn = personRepo.getOrCreateMrn(mrnStr, null, sourceSystem, observationTime, storedFrom);
        return returnCode;
    }
}
