package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * This class represents the association of Medical Resource Identifier (MRN) to
 * an individual patient (a Person).
 * <p>
 * Over the course of its lifetime a single MRN may be associated with any
 * number of patients. However, it may only be associated with a single patient
 * at any given point in history.
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(indexes = {
        @Index(name = "mrnIndex", columnList = "mrn", unique = false) })
public class Mrn implements Serializable {

    private static final long serialVersionUID = 939614930197714827L;

    /**
     * The MrnId is the UID for the association of an MRN value to a Person.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer             mrnId;

    @OneToMany(targetEntity = MrnEncounter.class, mappedBy = "mrn", cascade = CascadeType.ALL)
    private List<MrnEncounter> encounters;

    /**
     * The value of the MRN identifier.
     */
    @Column(unique = false, nullable = false)
    private String          mrn;
    private String          sourceSystem;

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant   createDatetime;

    /**
     * @return the mrnId
     */
    public Integer getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(Integer mrnId) {
        this.mrnId = mrnId;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the sourceSystem
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem the sourceSystem to set
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Add a new encounter to this MRN.
     *
     * @param enc the encounter to add
     * @param validFrom when the association became true
     * @param storedFrom when the association was stored
     */
    public void addEncounter(Encounter enc, Instant validFrom, Instant storedFrom) {
        if (this.encounters == null) {
            this.encounters = new ArrayList<>();
        }
        MrnEncounter mrnEncounter = new MrnEncounter(this, enc);
        mrnEncounter.setValidFrom(validFrom);
        mrnEncounter.setStoredFrom(storedFrom);
        this.encounters.add(mrnEncounter);
    }

    /**
     * @return the encounters
     */
    public List<MrnEncounter> getEncounters() {
        return encounters;
    }

    /**
     * @param encounters the encounters to set
     */
    public void setEncounters(List<MrnEncounter> encounters) {
        this.encounters = encounters;
    }

    @Override
    public String toString() {
        return "Mrn [mrnId=" + mrnId + ", encounters=" + encounters + ", mrn=" + mrn + ", sourceSystem=" + sourceSystem
                + "]";
    }

    /**
     * @return the Instant this Mrn was first recorded in the database
     */
    public Instant getCreateDatetime() {
        return createDatetime;
    }

    /**
     * @param createDatetime the Instant this Mrn was first recorded in the database
     */
    public void setCreateDatetime(Instant createDatetime) {
        this.createDatetime = createDatetime;
    }
}
