package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An MRN has multiple encounters, and an encounter may (over time) be
 * associated with several MRNs. Each MrnEncounter represents a single temporary
 * instance of such a relationship.
 *
 * @author Jeremy Stein
 *
 */
@Entity
@Table(name = "mrn_encounter")
@JsonIgnoreProperties({"mrn", "valid"})
public class MrnEncounter extends TemporalCore implements Serializable {

    private static final long serialVersionUID = 4153619042373632717L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer   mrnEncounterId;

    @ManyToOne
    @JoinColumn(name = "mrn", nullable = false)
    private Mrn       mrn;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "encounter", nullable = false)
    private Encounter encounter;

    /**
     * Create a new Mrn/Encounter association.
     */
    public MrnEncounter() {}

    /**
     * Create a new MRN/Encounter association.
     *
     * @param mrn the MRN
     * @param enc the Encounter
     */
    public MrnEncounter(Mrn mrn, Encounter enc) {
        this.mrn = mrn;
        this.encounter = enc;
    }

    /**
     * @return the MRN in the association
     */
    public Mrn getMrn() {
        return mrn;
    }

    /**
     * @return the Encounter in the association
     */
    public Encounter getEncounter() {
        return encounter;
    }

    /**
     * @return the mrnEncounterId
     */
    public Integer getMrnEncounterId() {
        return mrnEncounterId;
    }

    /**
     * @param mrnEncounterId the mrnEncounterId to set
     */
    public void setMrnEncounterId(Integer mrnEncounterId) {
        this.mrnEncounterId = mrnEncounterId;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(Mrn mrn) {
        this.mrn = mrn;
    }

    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    @Override
    public String toString() {
        return String.format("MrnEncounter [mrnEncounterId=%d, mrn=%d, encounter=%d]", mrnEncounterId, mrn.getMrnId(),
                encounter.getEncounterId());
    }
}
