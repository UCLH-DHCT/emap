package uk.ac.ucl.rits.inform.pipeline.exceptions;

/**
 * MRN was blank or couldn't be found.
 *
 * @author Jeremy Stein
 */
public class InvalidMrnException extends RuntimeException {

    private static final long serialVersionUID = 8164692590305671393L;

    /**
     * Create a new InvalidMrnException.
     *
     * @param message the message
     */
    public InvalidMrnException(String message) {
        super(message);
    }

}
