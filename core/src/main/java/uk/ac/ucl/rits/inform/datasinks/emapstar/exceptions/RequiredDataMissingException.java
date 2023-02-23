package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Thrown when an interchange message is missing a required data field.
 * @author Stef Piatek
 */
public class RequiredDataMissingException extends EmapOperationMessageProcessingException {

    private static final long serialVersionUID = -4161949863221660710L;

    /**
     * Create a new RequiredDataMissingException exception.
     * @param message the message
     */
    public RequiredDataMissingException(String message) {
        super(message);
    }

    /**
     * Each DB-loggable exception must describe itself.
     * @return the description
     */
    @Override
    public String getExceptionDescription() {
        return "Message is missing required information for it's purpose to be carried out";
    }
}
