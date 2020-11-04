package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

/**
 * This models the core temporal until values for an audit table.
 * <p>
 * These temporal columns form the core of Inform-DBs reproducibility
 * functionality. We record separately the time that this data was considered
 * true from the point of view of the hospital records (i.e. data loaded at a
 * later date may be considered true from when the original event happened), and
 * from when it was available in the database (so a repeat export of data won't
 * be contaminated by new data in reproducibility studies).
 * @author UCL RITS
 */
public interface AuditCore {

    /**
     * Get the time from when this bit of data was considered true or extant.
     * Historically loaded data is back dated to its original occurrence time.
     * @return the validFrom time and date.
     */
    Instant getValidFrom();

    /**
     * Get the time when this fact stopped being true within the hospital system.
     * This would occur if a correction was made, and the old value was now invalid.
     * @return the validUntil
     */
    Instant getValidUntil();

    /**
     * @param validUntil the validUntil to set
     */
    void setValidUntil(Instant validUntil);

    /**
     * @return the storedUntil
     */
    Instant getStoredUntil();

    /**
     * @param storedUntil the storedUntil to set
     */
    void setStoredUntil(Instant storedUntil);

    /**
     * Time-travel validity.
     * Note that this assumes the current state of the database is correct.
     * @param asOfTime The time to test validity at,
     *                 ie. the simulated "now" point. Cannot be null.
     * @return whether this row was valid as of the given time
     */
    default boolean isValidAsOf(Instant asOfTime) {
        return getStoredUntil() == null && asOfTime.compareTo(getValidFrom()) >= 0
                && (getValidUntil() == null || asOfTime.compareTo(getValidUntil()) < 0);
    }

}
