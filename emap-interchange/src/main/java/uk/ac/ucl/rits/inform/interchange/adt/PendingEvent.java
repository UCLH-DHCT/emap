package uk.ac.ucl.rits.inform.interchange.adt;

import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

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
