package uk.ac.ucl.rits.inform.pipeline.exceptions;

/**
 * During processing this HL7 message, we have decided not to make
 * any changes to the database. Not necessarily an error.
 *
 * @author Jeremy Stein
 *
 */
public class MessageIgnoredException extends Exception {

    private static final long serialVersionUID = 3654478669545317495L;

    /**
     * Create a new MessageIgnoredException.
     *
     * @param message the message
     */
    public MessageIgnoredException(String message) {
        super(message);
    }

}
