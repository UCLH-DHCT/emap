package uk.ac.ucl.rits.inform.interchange;

/**
 * Define the message types that an Emap processor
 * must process.
 *
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
     * @param msg the vital signs message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    String processMessage(VitalSigns msg) throws EmapOperationMessageProcessingException;
}
