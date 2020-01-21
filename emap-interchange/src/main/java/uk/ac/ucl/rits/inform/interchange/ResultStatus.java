package uk.ac.ucl.rits.inform.interchange;

/**
 * Result status of a HL7 message defines the action to be taken upon receipt of the message.
 */
public enum ResultStatus {
    /**
     * Add or update the record.
     */
    SAVE,
    /**
     * Delete the record.
     */
    DELETE
}
