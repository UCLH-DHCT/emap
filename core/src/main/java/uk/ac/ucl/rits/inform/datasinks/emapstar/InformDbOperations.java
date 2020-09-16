package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.AdtProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.FlowsheetProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.PathologyProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

import java.time.Instant;

/**
 * All the operations that can be performed on Inform-db.
 */
@Component
@EntityScan({"uk.ac.ucl.rits.inform.datasinks.emapstar.repos", "uk.ac.ucl.rits.inform.informdb"})
public class InformDbOperations implements EmapOperationMessageProcessor {
    @Autowired
    private AdtProcessor adtProcessor;
    @Autowired
    private FlowsheetProcessor flowsheetProcessor;
    @Autowired
    private PathologyProcessor pathologyProcessor;

    private static final Logger logger = LoggerFactory.getLogger(InformDbOperations.class);


    /**
     * Call when you are finished with this object.
     */
    public void close() {}


    /**
     * Process a pathology order message.
     * @param pathologyOrder the message
     * @return the return code
     * @throws EmapOperationMessageProcessingException if message could not be processed
     */
    @Override
    @Transactional
    public String processMessage(PathologyOrder pathologyOrder) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        pathologyProcessor.processMessage(pathologyOrder, storedFrom);
        // be more specific about the type of OK in future
        return "OK";
    }


    /**
     * @param msg the ADT message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public String processMessage(AdtMessage msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        return adtProcessor.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the MergeById message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public String processMessage(MergeById msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        return adtProcessor.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the DischargePatient message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public String processMessage(DischargePatient msg) throws EmapOperationMessageProcessingException {
        throw new MessageIgnoredException("Not implemented yet");
    }

    @Override
    @Transactional
    public String processMessage(VitalSigns msg) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        Instant storedFrom = Instant.now();
        flowsheetProcessor.processMessage(msg, storedFrom);
        return returnCode;
    }

    /**
     * @param msg the PatientInfection message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public String processMessage(PatientInfection msg) throws EmapOperationMessageProcessingException {
        return null;
    }

}
