package uk.ac.ucl.rits.inform.informdb.identity;

import java.time.Instant;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

/**
 * Over time MRNs are merged into others as more is found out about a patient.
 * This table stores a mapping from every MRN known to the system, to its
 * currently live (in use) MRN.
 * @author UCL RITS
 */
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(name = "mtl_mrn_id", columnList = "mrnId"),
        @Index(name = "live_mrn_id", columnList = "liveMrnId")})
@Data
@EqualsAndHashCode(callSuper = false)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AuditTable
public class MrnToLive extends TemporalCore<MrnToLive, MrnToLiveAudit> {

    /**
     * \brief Unique identifier in EMAP for this mrnToLive record.
     *
     * This is the primary key for the MrnToLive table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long mrnToLiveId;

    /**
     * \brief Identifier for the Mrn associated with this record.
     *
     * This is a foreign key that joins the mrnToLive table to the Mrn table.
     */
    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    /**
     * \brief Identifier for the Mrn associated with this record.
     *
     * This is a foreign key that joins the mrnToLive table to the Mrn table.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "liveMrnId", nullable = false)
    private Mrn liveMrnId;

    public MrnToLive() {}

    private MrnToLive(MrnToLive other) {
        super(other);
        setMrnId(other.getMrnId());
        setLiveMrnId(other.getLiveMrnId());
        this.mrnId = other.mrnId;
        this.liveMrnId = other.liveMrnId;
    }

    @Override
    public MrnToLive copy() {
        return new MrnToLive(this);
    }

    @Override
    public MrnToLiveAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        return new MrnToLiveAudit(this, validUntil, storedFrom);
    }

}
