package uk.ac.ucl.rits.inform.interchange;

import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;

/**
 * Define the message types that an Emap processor
 * must process.
 * @author Jeremy Stein
 */
public interface EmapOperationMessageProcessor {
    /**
     * @param msg the lab order message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(LabOrder msg) throws EmapOperationMessageProcessingException;

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
}
