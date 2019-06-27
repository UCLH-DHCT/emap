package uk.ac.ucl.rits.inform.exceptions;

/**
 * Multiple values/items were found in a situation where there should
 * only have been 0 or 1.
 *
 * @author Jeremy Stein
 */
public class DuplicateValueException extends RuntimeException {

    private static final long serialVersionUID = -649700514167561679L;

    /**
     * Create a duplicate value exception.
     *
     * @param message The message.
     */
    public DuplicateValueException(String message) {
        super(message);
    }
}
