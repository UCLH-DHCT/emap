package uk.ac.ucl.rits.inform.interchange.adt;

import java.time.Instant;

public interface AdtCancellationInterface {
    Instant getCancelledDateTime();

    void setCancelledDateTime(Instant cancelledDateTime);
}
