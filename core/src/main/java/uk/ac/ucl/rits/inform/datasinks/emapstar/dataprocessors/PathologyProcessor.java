package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonData;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;

import java.time.Instant;

/**
 * Handle processing of Pathology messages.
 * @author Stef Piatek
 */
@Component
public class PathologyProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonData personData;

    /**
     * @param personData person data.
     */
    public PathologyProcessor(PersonData personData) {
        this.personData = personData;
    }

    /**
     * Process Pathology message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @return return code
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public String processMessage(final PathologyOrder msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";

        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getObservationDateTime();
        Mrn mrn = personData.getOrCreateMrn(mrnStr, null, msg.getTestBatteryCodingSystem(), observationTime, storedFrom);
        return returnCode;
    }
}
