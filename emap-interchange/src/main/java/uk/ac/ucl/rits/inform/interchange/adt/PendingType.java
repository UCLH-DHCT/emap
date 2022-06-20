package uk.ac.ucl.rits.inform.interchange.adt;

/**
 * Type of pending transfer, to allow for a single table to track history of all pending changes.
 * @author Stef Piatek
 */
public enum PendingType {
    /**
     * Admit.
     */
    ADMIT,
    /**
     * Transfer for ADT messages: A15 and A26 (cancellation).
     */
    TRANSFER,
    /**
     * Discharge.
     */
    DISCHARGE
}
