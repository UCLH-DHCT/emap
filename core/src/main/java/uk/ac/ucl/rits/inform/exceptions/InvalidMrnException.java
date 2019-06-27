package uk.ac.ucl.rits.inform.exceptions;

/**
 * MRN was blank or couldn't be found when it really should have been.
 */
public class InvalidMrnException extends RuntimeException {
    /**
     * @param string the message
     */
    public InvalidMrnException(String string) {
        super(string);
    }

    /**
     */
    public InvalidMrnException() {
    }
}
