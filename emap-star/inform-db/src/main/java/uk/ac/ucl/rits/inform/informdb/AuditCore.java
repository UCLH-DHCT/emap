package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This models the core temporal until values for an audit table.
 * <p>
 * These temporal columns form the core of Inform-DBs reproducibility
 * functionality. We record separately the time that this data was considered
 * true from the point of view of the hospital records (i.e. data loaded at a
 * later date may be considered true from when the original event happened), and
 * from when it was available in the database (so a repeat export of data won't
 * be contaminated by new data in reproducibility studies).
 *
 * @author UCL RITS
 */
@SuppressWarnings("serial")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@MappedSuperclass
public abstract class AuditCore<A extends AuditCore<A>> extends TemporalCore<A, A> {

    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;

    /**
     * Default constructor.
     */
    public AuditCore() {}

    /**
     * Copy Constructor.
     * @param other Other object to copy fields from.
     */
    public AuditCore(AuditCore<A> other) {
        super(other);
        this.validUntil = other.validUntil;
        this.storedUntil = other.storedUntil;
    }

    /**
     * Construct with args
     * @param validUntil validUntil to set
     * @param storedUntil storedUntil to set
     */
    public AuditCore(Instant validUntil, Instant storedUntil) {
        this.validUntil = validUntil;
        this.storedUntil = storedUntil;
    }


    /**
     * Get the time when this fact stopped being true within the hospital system.
     * This would occur if a correction was made, and the old value was now invalid.
     *
     * @return the validUntil
     */
    public Instant getValidUntil() {
        return this.validUntil;
    }

    /**
     * Time-travel validity. Note that this assumes the current state of the
     * database is correct.
     *
     * @param asOfTime The time to test validity at, ie. the simulated "now" point.
     *                 Cannot be null.
     * @return whether this row was valid as of the given time
     */
    public boolean isValidAsOf(Instant asOfTime) {
        return getStoredUntil() == null && asOfTime.compareTo(getValidFrom()) >= 0
                && (getValidUntil() == null || asOfTime.compareTo(getValidUntil()) < 0);
    }

}
