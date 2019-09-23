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
     */
    void processMessage(PathologyOrder msg);
    /**
     * @param msg the ADT message to process
     */
    void processMessage(AdtMessage msg);
    /**
     * @param msg the vital signs message to process
     */
    void processMessage(VitalSigns msg);
}
