package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Something bad and unexpected has been discovered in Emap Star.
 *
 * @author Jeremy Stein
 */
public class EmapStarIntegrityException extends EmapOperationMessageProcessingException {

    private static final long serialVersionUID = 8821191007846604963L;

    /**
     * Create a new InformDbIntegrityException.
     *
     * @param message the message
     */
    public EmapStarIntegrityException(String message) {
        super(message);
    }

    @Override
    public String getExceptionDescription() {
        return "Contradiction between the database and the new message (or DB and itself)";
    }

}
