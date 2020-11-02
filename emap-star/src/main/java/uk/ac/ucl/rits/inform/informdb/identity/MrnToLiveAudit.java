package uk.ac.ucl.rits.inform.informdb.identity;

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
import java.time.Instant;

/**
 * Audit table of {@link MrnToLive}.
 * @author UCL RITS
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class MrnToLiveAudit extends MrnToLiveParent implements AuditCore<MrnToLiveParent> {

    private static final long serialVersionUID = 8891761742756656453L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long auditMrnToLiveId;
    private Long mrnToLiveId;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;


    public MrnToLiveAudit() {}

    public MrnToLiveAudit(MrnToLive originalEntity, Instant validUntil, Instant storedUntil) {
        super(originalEntity);
        this.validUntil = validUntil;
        this.storedUntil = storedUntil;
        mrnToLiveId = originalEntity.getMrnToLiveId();
    }

    private MrnToLiveAudit(MrnToLiveAudit other) {
        super(other);
    }

    @Override
    public MrnToLiveAudit copy() {
        return new MrnToLiveAudit(this);
    }
}
