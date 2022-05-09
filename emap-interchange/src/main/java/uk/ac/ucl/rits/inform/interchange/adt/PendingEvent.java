package uk.ac.ucl.rits.inform.interchange.adt;

import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

/**
 * Shared fields for all Pending Event interchange message types.
 * @author Stef Piatek
 */
public interface PendingEvent {
    /**
     * @return Type of pending event.
     */
    PendingType getPendingEventType();

    /**
     * @return Pending transfer location if known
     */
    InterchangeValue<String> getPendingLocation();

    /**
     * @param location concatenated location string
     */
    void setPendingLocation(InterchangeValue<String> location);
}
