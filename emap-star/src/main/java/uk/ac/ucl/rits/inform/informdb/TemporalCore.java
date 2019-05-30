package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * This models the common core of the temporal variables stored in almost all
 * Inform-DB tables.
 * <p>
 * These temporal columns form the core of Inform-DBs reproducibility
 * functionality. We record separately the time that this data was considered
 * true from the point of view of the hospital records (i.e. data loaded at a
 * later date may be considered true from when the original event happened), and
 * from when it was available in the database (so a repeat export of data won't
 * be contaminated by new data in reproducibility studies).
 *
 * @author UCL RITS
 *
 */
@MappedSuperclass
public abstract class TemporalCore {

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant validFrom;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant storedFrom;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;

    /**
     * Get the time from when this bit of data was considered true or extant.
     * Historically loaded data is back dated to its original occurrence time.
     *
     * @return the validFrom time and date.
     */
    public Instant getValidFrom() {
        return validFrom;
    }

    /**
     * Set the time from which this fact was considered true within the hospital
     * system.
     *
     * @param validFrom the validFrom to set
     */
    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    /**
     * Get the time when this fact stopped being true within the hospital system.
     * This would occur if a correction was made, and the old value was now invalid.
     *
     * @return the validUntil
     */
    public Instant getValidUntil() {
        return validUntil;
    }

    /**
     * @param validUntil the validUntil to set
     */
    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * @return the storedFrom
     */
    public Instant getStoredFrom() {
        return storedFrom;
    }

    /**
     * @param storedFrom the storedFrom to set
     */
    public void setStoredFrom(Instant storedFrom) {
        this.storedFrom = storedFrom;
    }

    /**
     * @return the storedUntil
     */
    public Instant getStoredUntil() {
        return storedUntil;
    }

    /**
     * @param storedUntil the storedUntil to set
     */
    public void setStoredUntil(Instant storedUntil) {
        this.storedUntil = storedUntil;
    }

}
