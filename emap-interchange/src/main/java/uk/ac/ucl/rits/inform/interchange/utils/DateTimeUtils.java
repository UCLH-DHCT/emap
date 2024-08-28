package uk.ac.ucl.rits.inform.interchange.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions for manipulating timestamps etc.
 */
public final class DateTimeUtils {
    private DateTimeUtils() {}

    /**
     * Round Instant to the nearest time unit using normal convention of halfway rounds up.
     * @param obsDatetime Instant to round
     * @param roundToUnit what ChronoUnit to round to, or null to return the original timestamp
     * @return the rounded Instant
     */
    public static Instant roundInstantToNearest(Instant obsDatetime, ChronoUnit roundToUnit) {
        if (roundToUnit == null) {
            return obsDatetime;
        }
        // determine whether to round up or down
        Instant roundedDown = obsDatetime.truncatedTo(roundToUnit);
        Instant roundedUp = obsDatetime.plus(1, roundToUnit).truncatedTo(roundToUnit);
        if (obsDatetime.until(roundedUp, ChronoUnit.NANOS) > roundedDown.until(obsDatetime, ChronoUnit.NANOS)) {
            return roundedDown;
        } else {
            return roundedUp;
        }
    }

}
