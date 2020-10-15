package uk.ac.ucl.rits.inform.interchange;

import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;

/**
 * Define the message types that an Emap processor
 * must process.
 * @author Jeremy Stein
 */
public interface EmapOperationMessageProcessor {
    /**
     * @param msg the pathology order message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(PathologyOrder msg) throws EmapOperationMessageProcessingException;

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
     * @param msg the vital signs message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(VitalSigns msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the PatientInfection message to process
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    void processMessage(PatientInfection msg) throws EmapOperationMessageProcessingException;
}
