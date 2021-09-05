package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.LocationMetadataController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.AdtProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.AdvancedDecisionProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.ConsultationRequestProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.FlowsheetProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.LabProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.PatientStateProcessor;
import uk.ac.ucl.rits.inform.interchange.AdvancedDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

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
    private LabProcessor labProcessor;
    @Autowired
    private PatientStateProcessor patientStateProcessor;
    @Autowired
    private ConsultationRequestProcessor consultationRequestProcessor;
    @Autowired
    private LocationMetadataController locationMetadataController;
    @Autowired
    private AdvancedDecisionProcessor advancedDecisionProcessor;

    /**
     * Process a lab order message.
     * @param labOrderMsg the message
     * @throws EmapOperationMessageProcessingException if message could not be processed
     */
    @Override
    @Transactional
    public void processMessage(LabOrderMsg labOrderMsg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        labProcessor.processMessage(labOrderMsg, storedFrom);
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
    @Transactional
    public void processMessage(MoveVisitInformation msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.moveVisitInformation(msg, storedFrom);
    }

    /**
     * @param msg the ChangePatientIdentifiers message to process
     */
    @Override
    @Transactional
    public void processMessage(ChangePatientIdentifiers msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        adtProcessor.changePatientIdentifiers(msg, storedFrom);
    }

    /**
     * @param msg the SwapLocations message to process
     */
    @Override
    @Transactional
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
    @Transactional
    public void processMessage(PatientInfection msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        patientStateProcessor.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the FlowsheetMetadata message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    @Transactional
    public void processMessage(FlowsheetMetadata msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        flowsheetProcessor.processMessage(msg, storedFrom);
    }

    @Override
    @Transactional
    public void processMessage(ConsultRequest msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        consultationRequestProcessor.processMessage(msg, storedFrom);
    }

    @Override
    @Transactional
    public void processMessage(LocationMetadata msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        locationMetadataController.processMessage(msg, storedFrom);
    }

    @Override
    @Transactional
    public void processMessage(AdvancedDecisionMessage msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        advancedDecisionProcessor.processMessage(msg, storedFrom);
    }
}
