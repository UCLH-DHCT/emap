package uk.ac.ucl.rits.inform.interchange;

/**
 * Define the message types that an Emap processor
 * must process.
 * 
 * @author Jeremy Stein
 *
 */
public interface EmapOperationMessageProcessor {
    public void processMessage(PathologyOrder msg);
    public void processMessage(AdtMessage msg);
}
