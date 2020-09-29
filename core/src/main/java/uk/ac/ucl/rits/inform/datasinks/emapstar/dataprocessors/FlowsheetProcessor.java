package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
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
    private final PersonController personController;

    /**
     * @param personController person data.
     */
    public FlowsheetProcessor(PersonController personController) {
        this.personController = personController;
    }

    /**
     * Process vitalsigns message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @return return code
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public String processMessage(final VitalSigns msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getObservationTimeTaken();
        Mrn mrn = personController.getOrCreateMrn(mrnStr, null, msg.getSourceSystem(), observationTime, storedFrom);
        return returnCode;
    }
}
