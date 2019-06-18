package uk.ac.ucl.rits.inform.exceptions;

/**
 * Something has gone wrong with one of our attributes.
 * @author Jeremy Stein
 *
 */
public class AttributeError extends RuntimeException {
    /**
     * @param message error message
     */
    public AttributeError(String message) {
        super(message);
    }
}
