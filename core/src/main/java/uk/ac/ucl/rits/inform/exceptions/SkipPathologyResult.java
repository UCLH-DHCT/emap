package uk.ac.ucl.rits.inform.exceptions;

/**
 * Not an error but a pathology result that we don't handle yet.
 * @author Jeremy Stein
 *
 */
public class SkipPathologyResult extends RuntimeException {
    /**
     * @param string the message
     */
    public SkipPathologyResult(String string) {
        super(string);
    }
}
