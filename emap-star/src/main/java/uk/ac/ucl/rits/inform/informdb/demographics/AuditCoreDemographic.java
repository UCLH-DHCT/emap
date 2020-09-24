package uk.ac.ucl.rits.inform.informdb.demographics;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.time.Instant;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditCoreDemographic extends CoreDemographic implements AuditCore<CoreDemographic> {
    private static final long serialVersionUID = -8516988957488992519L;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant validFrom;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant storedFrom;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;

    /**
     * Default constructor.
     */
    public AuditCoreDemographic() {
    }

    /**
     * Constructor from original entity to be audit logged.
     * @param originalEntity original entity.
     */
    public AuditCoreDemographic(CoreDemographic originalEntity) {
        super(originalEntity);
    }

    @Override
    public AuditCore<CoreDemographic> buildFromOriginalEntity(CoreDemographic originalEntity) {
        return new AuditCoreDemographic(originalEntity);
    }
}
