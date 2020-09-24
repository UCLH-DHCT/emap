package uk.ac.ucl.rits.inform.informdb.identity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * Over time MRNs are merged into others as more is found out about a patient.
 * This table stores a mapping from every MRN known to the system, to its
 * currently live (in use) MRN.
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(name = "mrn_to_live")
public class MrnToLive extends TemporalCore<MrnToLive> implements Serializable {

    private static final long serialVersionUID = 7019692664925413320L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long              mrnToLiveId;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn               mrnId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "liveMrnId", nullable = false)
    private Mrn               liveMrnId;

    public MrnToLive() {}

    public MrnToLive(MrnToLive other) {
        super(other);
        this.mrnId = other.mrnId;
        this.liveMrnId = other.liveMrnId;
    }

    /**
     * @return the mrnToLiveId
     */
    public Long getMrnToLiveId() {
        return mrnToLiveId;
    }

    /**
     * @param mrnToLiveId the mrnToLiveId to set
     */
    public void setMrnToLiveId(Long mrnToLiveId) {
        this.mrnToLiveId = mrnToLiveId;
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

    @Override
    public MrnToLive copy() {
        return new MrnToLive(this);
    }

}
