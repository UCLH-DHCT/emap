package uk.ac.ucl.rits.inform.informdb;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * An MRN has multiple encounters,
 * and an encounter may (over time) be associated
 * with several MRNs.
 * @author jeremystein
 *
 */
@Entity
@Table(name = "mrn_encounter")
public class MrnEncounter extends TemporalCore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer mrnEncounterid;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "mrn", nullable = false)
    private Mrn mrn;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "encounter", nullable = false)
    private Encounter encounter;

    /**
     * .
     */
    public MrnEncounter() {
    }

    /**
     * create a new MRN/Encounter association.
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

    @Override
    public String toString() {
        return "MrnEncounter [mrnEncounterid=" + mrnEncounterid + ", mrn=" + mrn.getMrnId() + ", encounter=" + encounter.getEncounterId()
                + "]";
    }
}
