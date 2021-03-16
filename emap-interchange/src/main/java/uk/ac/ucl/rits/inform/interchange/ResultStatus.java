package uk.ac.ucl.rits.inform.interchange;

/**
 * Result status of an Interchange message defines the action to be taken when parsing.
 */
public enum ResultStatus {
    /**
     * Add or update the field.
     */
    SAVE,
    /**
     * Delete the field.
     */
    DELETE,
    /**
     * Field has no data so should not be modified. Aka "unknown".
     */
    IGNORE
}
