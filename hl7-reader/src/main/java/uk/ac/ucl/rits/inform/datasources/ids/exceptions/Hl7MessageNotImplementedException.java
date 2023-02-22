package uk.ac.ucl.rits.inform.datasources.ids.exceptions;

/**
 * Exception for when we have not implemented a particular HL7 message type/event.
 * @author Jeremy Stein
 *
 */
public class Hl7MessageNotImplementedException extends Exception {
    /**
     * @param message error message
     */
    public Hl7MessageNotImplementedException(String message) {
        super(message);
    }
}
