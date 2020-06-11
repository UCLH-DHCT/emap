package uk.ac.ucl.rits.inform.datasources.ids.exceptions;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * During processing this message, we have decided not to make
 * any changes to the database. Not necessarily an error.
 *
 * @author Jeremy Stein
 *
 */
public class Hl7MessageIgnoredException extends EmapOperationMessageProcessingException {

    private static final long serialVersionUID = 3654478669545317495L;
    private boolean flagFollowUp;

    /**
     * Create a new MessageIgnoredException.
     *
     * @param errorMessage a string error message
     */
    @Deprecated
    public Hl7MessageIgnoredException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Create a new MessageIgnoredException with some extra info from the message.
     *
     * @param msg the interchange message
     * @param errorMessage a string error message
     */
    public Hl7MessageIgnoredException(EmapOperationMessage msg, String errorMessage) {
        super(errorMessage);
    }

    @Override
    public String getExceptionDescription() {
        return "Message can probably be skipped";
    }

    /**
     * @return whether this exception has been flagged to follow up
     */
    public boolean isFlagFollowUp() {
        return flagFollowUp;
    }

    /**
     * Flag in the log the message which triggered this exception as an error
     * worthy of follow up. Eg. an unexpected feature of the
     * data that will require a developer to investigate.
     *
     * @param flagFollowUp true iff should flag for follow up
     */
    public void setFlagFollowUp(boolean flagFollowUp) {
        this.flagFollowUp = flagFollowUp;
    }
}
