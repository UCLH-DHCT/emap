package uk.ac.ucl.rits.inform.exceptions;

/**
 * Something bad and unexpected has been discovered in Inform-db.
 *
 * @author Jeremy Stein
 */
public class InformDbIntegrityException extends RuntimeException {

    private static final long serialVersionUID = 8821191007846604963L;

    /**
     * Create a new InformDbIntegrityException.
     *
     * @param message the message
     */
    public InformDbIntegrityException(String message) {
        super(message);
    }

}
