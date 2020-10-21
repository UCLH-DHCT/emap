package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * During processing this message, we have decided not to make any changes to the database.
 * Message was malformed or the state of the database doesn't allow for the message's intent to be carried out.
 * @author Jeremy Stein
 */
public class MessageIgnoredException extends EmapOperationMessageProcessingException {

    private static final long serialVersionUID = 3654478669545317495L;

    /**
     * Create a new MessageIgnoredException.
     * @param errorMessage a string error message
     */
    public MessageIgnoredException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Create a new MessageIgnoredException with some extra info from the message.
     * @param msg          the interchange message
     * @param errorMessage a string error message
     */
    public MessageIgnoredException(EmapOperationMessage msg, String errorMessage) {
        super(errorMessage);
    }

    @Override
    public String getExceptionDescription() {
        return "Message can probably be skipped";
    }
}
