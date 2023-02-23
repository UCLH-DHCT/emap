package uk.ac.ucl.rits.inform.datasources.ids.exceptions;

/**
 * There's an anomaly which looks likely to be caused by
 * a fault in the HL7 data. Eg. We got an A13 cancel discharge
 * but we don't have a record of a discharge in the first place.
 * It could be hard to distinguish from cases where we should throw
 * an InformDbIntegrityException (eg. how do we know whether we didn't
 * record the discharge properly or we were never notified of it?)
 *
 * Whenever this exception is thrown, it should be recorded and the
 * cause investigated.
 */
public class Hl7InconsistencyException extends Exception {
    /**
     * @param message the message
     */
    public Hl7InconsistencyException(String message) {
        super(message);
    }

    /**
     * @param cause the cause
     */
    public Hl7InconsistencyException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message the message
     * @param cause the cause
     */
    public Hl7InconsistencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
