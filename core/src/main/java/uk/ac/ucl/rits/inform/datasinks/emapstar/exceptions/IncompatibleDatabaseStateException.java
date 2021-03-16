package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * The state of the database means that a message can not have the desired effect.
 *
 * @author Jeremy Stein & Stef Piatek
 */
public class IncompatibleDatabaseStateException extends EmapOperationMessageProcessingException {

    private static final long serialVersionUID = 8821191007846604963L;

    /**
     * Create a new InformDbIntegrityException.
     *
     * @param message the message
     */
    public IncompatibleDatabaseStateException(String message) {
        super(message);
    }

    @Override
    public String getExceptionDescription() {
        return "Contradiction between the database and the new message";
    }

}
