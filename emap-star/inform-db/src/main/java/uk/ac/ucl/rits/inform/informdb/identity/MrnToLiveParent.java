package uk.ac.ucl.rits.inform.informdb.identity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

/**
 * Parent class that is not created as an entity to avoid polymorphic queries based on the original and audit table.
 * see {@link MrnToLive} for more details
 * @author UCL RITS
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AuditTable
public abstract class MrnToLiveParent extends TemporalCore<MrnToLiveParent> implements Serializable {

    private static final long serialVersionUID = 7019692664925413320L;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "liveMrnId", nullable = false)
    private Mrn liveMrnId;

    public MrnToLiveParent() {}

    public MrnToLiveParent(MrnToLiveParent other) {
        super(other);
        this.mrnId = other.mrnId;
        this.liveMrnId = other.liveMrnId;
    }

    /**
     * @return the mrnId
     */
    public Mrn getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(Mrn mrnId) {
        this.mrnId = mrnId;
    }

    /**
     * @return the liveMrnId
     */
    public Mrn getLiveMrnId() {
        return liveMrnId;
    }

    /**
     * @param liveMrnId the liveMrnId to set
     */
    public void setLiveMrnId(Mrn liveMrnId) {
        this.liveMrnId = liveMrnId;
    }

    @Override
    public String toString() {
        return String.format("MrnToLive [mrn_id=%s, live_mrn_id=%s]", mrnId, liveMrnId);
    }

}
