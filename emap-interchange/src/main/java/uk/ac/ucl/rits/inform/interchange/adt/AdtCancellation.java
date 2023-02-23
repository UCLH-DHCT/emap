package uk.ac.ucl.rits.inform.interchange.adt;

import java.time.Instant;

/**
 * Additional data requirements for a cancellation adt message.
 */
public interface AdtCancellation {
    /**
     * @return cancelledDatetime.
     */
    Instant getCancelledDateTime();

    /**
     * @param cancelledDateTime to set.
     */
    void setCancelledDateTime(Instant cancelledDateTime);
}
