package uk.ac.ucl.rits.inform.exceptions;

/**
 * During processing this HL7 message, we have decided not to make
 * any changes to the database. Not necessarily an error.
 * The meaning of this will likely evolve over time...
 * @author jeremystein
 *
 */
public class MessageIgnoredException extends RuntimeException {

    /**
     * @param string a message
     */
    public MessageIgnoredException(String string) {
        super(string);
    }

}
