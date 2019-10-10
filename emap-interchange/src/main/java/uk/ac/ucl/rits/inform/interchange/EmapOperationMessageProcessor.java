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
     */
    String processMessage(PathologyOrder msg);
    /**
     * @param msg the ADT message to process
     * @return return code
     */
    String processMessage(AdtMessage msg);
    /**
     * @param msg the vital signs message to process
     * @return return code
     */
    String processMessage(VitalSigns msg);
}
