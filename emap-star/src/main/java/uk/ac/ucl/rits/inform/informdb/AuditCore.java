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
 * @param <T> Original persistence class.
 * @author UCL RITS
 */
public interface AuditCore<T extends TemporalCore<T>> {

    /**
     * Get the time from when this bit of data was considered true or extant.
     * Historically loaded data is back dated to its original occurrence time.
     * @return the validFrom time and date.
     */
    Instant getValidFrom();

    /**
     * Set the time from which this fact was considered true within the hospital
     * system.
     * @param validFrom the validFrom to set
     */
    void setValidFrom(Instant validFrom);

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
     * @return the storedFrom
     */
    Instant getStoredFrom();

    /**
     * @param storedFrom the storedFrom to set
     */
    void setStoredFrom(Instant storedFrom);

    /**
     * @return the storedUntil
     */
    Instant getStoredUntil();

    /**
     * @param storedUntil the storedUntil to set
     */
    void setStoredUntil(Instant storedUntil);

    /**
     * Builds audit table from original entity with all values except for storedUntil and validUntil.
     * @param originalEntity Original entity.
     * @return audit table
     */
    AuditCore<T> buildFromOriginalEntity(T originalEntity);

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


    /**
     * Build and return an audit row for the original table.
     * @param originalEntity original entity to be audited.
     * @param storedUntil    the time that this change is being made in the DB
     * @param validUntil     the time at which this fact stopped being true,
     *                       can be any amount of time in the past
     * @return the newly created row
     */
    default AuditCore<T> createAuditRow(T originalEntity, Instant storedUntil, Instant validUntil) {
        AuditCore<T> audit = buildFromOriginalEntity(originalEntity);
        audit.setStoredUntil(storedUntil);
        audit.setValidUntil(validUntil);
        return audit;
    }
}
