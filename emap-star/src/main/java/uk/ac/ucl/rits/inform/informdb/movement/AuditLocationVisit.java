package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Audit table of {@link LocationVisit}.
 */
@Entity
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLocationVisit extends LocationVisitParent implements AuditCore<LocationVisitParent> {
    private static final long serialVersionUID = 5021782039578121716L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long auditLocationVisitId;
    @Column(nullable = false)
    private long locationVisitId;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;


    /**
     * Default constructor.
     */
    public AuditLocationVisit() {
    }

    /**
     * Constructor from original entity and invalidation times.
     * @param originalEntity original entity to be audited.
     * @param storedUntil    the time that this change is being made in the DB
     * @param validUntil     the time at which this fact stopped being true,
     *                       can be any amount of time in the past
     */
    public AuditLocationVisit(final LocationVisit originalEntity, final Instant validUntil, final Instant storedUntil) {
        super(originalEntity);
        this.validUntil = validUntil;
        this.storedUntil = storedUntil;
        this.locationVisitId = originalEntity.getLocationVisitId();
    }
}
