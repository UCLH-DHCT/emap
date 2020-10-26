package uk.ac.ucl.rits.inform.informdb.identity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.time.Instant;

/**
 * Audit table of {@link HospitalVisit}.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class AuditHospitalVisit extends HospitalVisitParent implements AuditCore<HospitalVisitParent> {
    private static final long serialVersionUID = -8516988957488992519L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long auditHospitalVisitId;
    @Column(nullable = false)
    private long hospitalVisitId;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;
    @Column(nullable = false)
    private String encounter;


    /**
     * Default constructor.
     */
    public AuditHospitalVisit() {
    }

    private AuditHospitalVisit(AuditHospitalVisit other) {
        super(other);
    }


    /**
     * Constructor from original entity and invalidation times.
     * @param originalEntity original entity to be audited.
     * @param storedUntil    the time that this change is being made in the DB
     * @param validUntil     the time at which this fact stopped being true,
     *                       can be any amount of time in the past
     */
    public AuditHospitalVisit(final HospitalVisit originalEntity, final Instant validUntil, final Instant storedUntil) {
        super(originalEntity);
        this.validUntil = validUntil;
        this.storedUntil = storedUntil;
        if (originalEntity.getHospitalVisitId() != null) {
            // Newly created visit won't have an ID, audit row should never be saved in that case - not setting it should be okay.
            this.hospitalVisitId = originalEntity.getHospitalVisitId();
        }
        this.encounter = originalEntity.getEncounter();
    }

    @Override
    public AuditHospitalVisit copy() {
        return new AuditHospitalVisit(this);
    }
}
