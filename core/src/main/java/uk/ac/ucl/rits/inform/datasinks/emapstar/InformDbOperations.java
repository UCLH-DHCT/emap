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
import uk.ac.ucl.rits.inform.interchange.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;

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
     * @throws EmapOperationMessageProcessingException if message could not be processed
     */
    @Override
    @Transactional
    public void processMessage(PathologyOrder pathologyOrder) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        pathologyProcessor.processMessage(pathologyOrder, storedFrom);
    }


    /**
     * @param msg the ADT message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public void processMessage(AdtMessage msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the MergeById message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public void processMessage(MergePatient msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.processMergePatient(msg, storedFrom);
    }

    /**
     * @param msg the DischargePatient message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public void processMessage(DeletePersonInformation msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.deletePersonInformation(msg, storedFrom);
    }

    /**
     * @param msg the MoveVisitInformation message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(MoveVisitInformation msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.moveVisitInformation(msg, storedFrom);
    }

    /**
     * @param msg the ChangePatientIdentifiers message to process
     */
    @Override
    public void processMessage(ChangePatientIdentifiers msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.changePatientIdentifiers(msg, storedFrom);
    }

    /**
     * @param msg the SwapLocations message to process
     */
    @Override
    public void processMessage(SwapLocations msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.swapLocations(msg, storedFrom);
    }

    @Override
    @Transactional
    public void processMessage(Flowsheet msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        flowsheetProcessor.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the PatientInfection message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(PatientInfection msg) throws EmapOperationMessageProcessingException {
        throw new MessageIgnoredException("Not implemented yet");
    }

}
