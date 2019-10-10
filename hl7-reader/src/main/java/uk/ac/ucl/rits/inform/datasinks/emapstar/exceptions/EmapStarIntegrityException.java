package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

/**
 * Something bad and unexpected has been discovered in Emap Star.
 *
 * @author Jeremy Stein
 */
public class EmapStarIntegrityException extends Exception {

    private static final long serialVersionUID = 8821191007846604963L;

    /**
     * Create a new InformDbIntegrityException.
     *
     * @param message the message
     */
    public EmapStarIntegrityException(String message) {
        super(message);
    }

}
