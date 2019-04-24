package uk.ac.ucl.rits.inform;

/**
 * During processing this HL7 message, we have decided not to make
 * any changes to the database. Not necessarily an error.
 * The meaning of this will likely evolve over time...
 * @author jeremystein
 *
 */
public class MessageIgnoredException extends RuntimeException {

    public MessageIgnoredException(String string) {
        super(string);
    }

}
