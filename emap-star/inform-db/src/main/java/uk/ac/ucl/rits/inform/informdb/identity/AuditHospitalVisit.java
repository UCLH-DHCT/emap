package uk.ac.ucl.rits.inform.informdb.identity;

import java.time.Instant;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

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
@AttributeOverride(name = "encounter", column = @Column(unique = false) )
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


    /**
     * Default constructor.
     */
    public AuditHospitalVisit() {
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
        this.hospitalVisitId = originalEntity.getHospitalVisitId();
    }
}
