package uk.ac.ucl.rits.inform.interchange;

/**
 * Describes an exception which can be logged in our message logging table in the destination database.
 *
 * @author Jeremy Stein
 */
public abstract class EmapOperationMessageProcessingException extends Exception {

    private EmapOperationMessage msg;

    /**
     * Exception with a message.
     * @param message the message
     */
    public EmapOperationMessageProcessingException(String message) {
        super(message);
    }

    /**
     * Get the "return code" for the DB logging.
     * @return just the class name
     */
    public String getReturnCode() {
        return this.getClass().getSimpleName();
    }

    /**
     * @return the brief message details for logging purposes eg. "[AdtFoo 00012345]"
     */
    public String getErrorMessagePreamble() {
        return String.format("[%s %s]", msg.getMessageType(), msg.getSourceMessageId());
    }

    /**
     * Each DB-loggable exception must describe itself.
     * @return the description
     */
    public abstract String getExceptionDescription();
}
