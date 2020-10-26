package uk.ac.ucl.rits.inform.informdb;

import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

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
 * @param <T> The Class type returned by the invalidation method.
 * @author UCL RITS
 */
@MappedSuperclass
public abstract class TemporalCore<T extends TemporalCore<T>> {

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant validFrom;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant storedFrom;

    /**
     * Default constructor.
     */
    public TemporalCore() {
    }

    /**
     * Copy constructor.
     * @param other object to copy from
     */
    public TemporalCore(TemporalCore<T> other) {
        validFrom = other.validFrom;
        storedFrom = other.storedFrom;
    }

    /**
     * Get the time from when this bit of data was considered true or extant.
     * Historically loaded data is back dated to its original occurrence time.
     * @return the validFrom time and date.
     */
    public Instant getValidFrom() {
        return validFrom;
    }

    /**
     * Set the time from which this fact was considered true within the hospital
     * system.
     * @param validFrom the validFrom to set
     */
    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
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


    public abstract T copy();

}
