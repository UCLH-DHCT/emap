package uk.ac.ucl.rits.inform.informdb;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * This is an association of a single encounter with a MRN.
 * <p>
 * Note that the same encounter may be present multiple times in this table with
 * different associated MRNs (with different valid from - until).
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(indexes = { @Index(name = "validUntilIndex", columnList = "validUntil", unique = false),
        @Index(name = "encounterIndex", columnList = "encounter", unique = false) })
public class Encounter extends TemporalCore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int       encounterId;

    @ManyToOne
    @JoinColumn()
    private Mrn       mrn;

    private String    encounter;
    private String    sourceSystem;

    @ManyToOne
    @JoinColumn()
    private Encounter parentEncounter;

    /**
     * @return the encounterId
     */
    public int getEncounterId() {
        return encounterId;
    }

    /**
     * @param encounterId the encounterId to set
     */
    public void setEncounterId(int encounterId) {
        this.encounterId = encounterId;
    }

    /**
     * @return the mrn
     */
    public Mrn getMrn() {
        return mrn;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(Mrn mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the encounter
     */
    public String getEncounter() {
        return encounter;
    }

    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(String encounter) {
        this.encounter = encounter;
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
     * @return the parentEncounter
     */
    public Encounter getParentEncounter() {
        return parentEncounter;
    }

    /**
     * @param parentEncounter the parentEncounter to set
     */
    public void setParentEncounter(Encounter parentEncounter) {
        this.parentEncounter = parentEncounter;
    }

    @Override
    public String toString() {
        return "Encounter [encounter_id=" + encounterId + ", mrn=" + mrn + ", encounter=" + encounter
                + ", store_datetime=" + this.getStoredFrom() + ", end_datetime=" + this.getValidUntil()
                + ", source_system=" + sourceSystem + ", event_time=" + this.getValidFrom() + "]";
    }

}
