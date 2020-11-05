package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import lombok.Data;

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
 * @param <T> The Entity Class type returned by the copy method.
 * @param <A> The Audit Entity Class type returned by the getAuditEntity method.
 * @author UCL RITS
 */
@SuppressWarnings("serial")
@Data
@MappedSuperclass
public abstract class TemporalCore<T extends TemporalCore<T, A>, A extends AuditCore> implements Serializable {

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant validFrom;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant storedFrom;

    /**
     * Default constructor.
     */
    protected TemporalCore() {
    }

    /**
     * Copy constructor.
     * @param other object to copy from
     */
    protected TemporalCore(TemporalCore<T, A> other) {
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

    public abstract T copy();

    /**
     * @param validUntil the event time that invalidated the current state
     * @param storedFrom the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    public abstract A createAuditEntity(Instant validUntil, Instant storedFrom);
}
