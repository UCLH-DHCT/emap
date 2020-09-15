package uk.ac.ucl.rits.inform.interchange;

import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

/**
 * Define the message types that an Emap processor
 * must process.
 * @author Jeremy Stein
 */
public interface EmapOperationMessageProcessor {
    /**
     * @param msg the pathology order message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(PathologyOrder msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the ADT message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(AdtMessage msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the MergeById message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(MergeById msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the DischargePatient message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(DischargePatient msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the vital signs message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(VitalSigns msg) throws EmapOperationMessageProcessingException;

    /**
     * @param msg the PatientInfection message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(PatientInfection msg);
}
