package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Audit table of {@link LocationVisit}.
 */
@Entity
@Table
@Data
@EqualsAndHashCode(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class LocationVisitAudit extends LocationVisitParent implements AuditCore {
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
    private long hospitalVisitId;


    /**
     * Default constructor.
     */
    public LocationVisitAudit() {
    }

    /**
     * Constructor from original entity and invalidation times.
     * @param originalEntity original entity to be audited.
     * @param storedUntil    the time that this change is being made in the DB
     * @param validUntil     the time at which this fact stopped being true,
     *                       can be any amount of time in the past
     */
    public LocationVisitAudit(final LocationVisit originalEntity, final Instant validUntil, final Instant storedUntil) {
        super(originalEntity);
        this.validUntil = validUntil;
        this.storedUntil = storedUntil;
        locationVisitId = originalEntity.getLocationVisitId();
        hospitalVisitId = originalEntity.getHospitalVisitId().getHospitalVisitId();
    }

    @Override
    public LocationVisit copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocationVisitAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        throw new UnsupportedOperationException();
    }
}
