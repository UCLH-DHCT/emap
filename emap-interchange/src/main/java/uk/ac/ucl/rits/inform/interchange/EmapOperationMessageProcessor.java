package uk.ac.ucl.rits.inform.interchange;

import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

/**
 * Define the message types that an Emap processor
 * must process.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public interface EmapOperationMessageProcessor {
    /**
     * @param msg the lab order message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(LabOrderMsg msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the ADT message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(AdtMessage msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the MergeById message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(MergePatient msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the DeletePersonInformation message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(DeletePersonInformation msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the MoveVisitInformation message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(MoveVisitInformation msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the ChangePatientIdentifiers message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(ChangePatientIdentifiers msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the SwapLocations to process
     * @throws EmapOperationMessageProcessingException if the message cannot be processed
     */
    void processMessage(SwapLocations msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the flowsheet message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(Flowsheet msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the PatientInfection message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(PatientInfection msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the FlowsheetMetadata message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(FlowsheetMetadata msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the LocationMetadata message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(LocationMetadata msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the ConsultRequest message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(ConsultRequest msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the AdvancedDecisionMessage message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(AdvancedDecisionMessage msg) throws EmapOperationMessageProcessingException;
}
