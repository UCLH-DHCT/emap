package uk.ac.ucl.rits.inform.exceptions;

/**
 * Something has gone wrong with one of our attributes.
 *
 * @author Jeremy Stein
 *
 */
public class AttributeError extends RuntimeException {

    private static final long serialVersionUID = 6315281513319420402L;

    /**
     * Create a new Attribute error.
     *
     * @param message error message
     */
    public AttributeError(String message) {
        super(message);
    }
}
