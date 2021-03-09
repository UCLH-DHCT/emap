package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

public class MessageCancelledException extends EmapOperationMessageProcessingException {
    private static final long serialVersionUID = 1011548393240551796L;

    /**
     * Exception with a message.
     * @param message the message
     */
    public MessageCancelledException(String message) {
        super(message);
    }

    /**
     * Each DB-loggable exception must describe itself.
     * @return the description
     */
    @Override
    public String getExceptionDescription() {
        return "Message outcome was cancelled before this message was received";
    }
}
