package uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * MRN was blank or couldn't be found.
 *
 * @author Jeremy Stein
 */
public class InvalidMrnException extends EmapOperationMessageProcessingException {

    private static final long serialVersionUID = 8164692590305671393L;

    /**
     * Create a new InvalidMrnException.
     *
     * @param message the message
     */
    public InvalidMrnException(String message) {
        super(message);
    }

    @Override
    public String getExceptionDescription() {
        return "";
    }

}
