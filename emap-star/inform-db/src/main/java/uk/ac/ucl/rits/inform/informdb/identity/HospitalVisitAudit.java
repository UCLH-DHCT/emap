package uk.ac.ucl.rits.inform.informdb.identity;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

/**
 * Audit table of {@link HospitalVisit}.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class HospitalVisitAudit extends HospitalVisitParent implements AuditCore {
    private static final long serialVersionUID = -8516988957488992519L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long              auditHospitalVisitId;
    @Column(nullable = false)
    private long              hospitalVisitId;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           validUntil;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           storedUntil;

    /**
     * Default constructor.
     */
    public HospitalVisitAudit() {}

    private HospitalVisitAudit(HospitalVisitAudit other) {
        super(other);
    }

    /**
     * Constructor from original entity and invalidation times.
     *
     * @param originalEntity original entity to be audited.
     * @param storedUntil    the time that this change is being made in the DB
     * @param validUntil     the time at which this fact stopped being true, can be
     *                       any amount of time in the past
     */
    public HospitalVisitAudit(final HospitalVisit originalEntity, final Instant validUntil, final Instant storedUntil) {
        super(originalEntity);
        this.validUntil = validUntil;
        this.storedUntil = storedUntil;
        this.hospitalVisitId = originalEntity.getHospitalVisitId();
        encounter = originalEntity.getEncounter();
    }

    @Override
    public HospitalVisit copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HospitalVisitAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        throw new UnsupportedOperationException();
    }
}
