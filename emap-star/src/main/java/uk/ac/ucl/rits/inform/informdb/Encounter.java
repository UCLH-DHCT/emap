package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
public class Encounter extends TemporalCore implements Serializable {

    private static final long            serialVersionUID = -915410794459512129L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int                          encounterId;

    @Column(unique = false, nullable = false)
    private String                       encounter;
    private String                       sourceSystem;

    @ManyToOne(targetEntity = Encounter.class)
    @JoinColumn(name = "parent_encounter")
    private Encounter                    parentEncounter;

    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL)
    private List<PatientDemographicFact> demographics;

    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL)
    private List<VisitFact>              visits;

    @OneToMany(targetEntity = MrnEncounter.class, mappedBy = "encounter", cascade = CascadeType.ALL)
    private List<MrnEncounter> mrns;

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

    /**
     * Add a patient demographic fact to this encounter.
     *
     * @param fact The fact to add
     */
    public void addDemographic(PatientDemographicFact fact) {
        if (this.demographics == null) {
            this.demographics = new ArrayList<>();
        }
        this.demographics.add(fact);
        fact.setEncounter(this);
    }

    /**
     * Add a visit fact to this encounter.
     *
     * @param fact The fact to add
     */
    public void addVisit(VisitFact fact) {
        if (this.visits == null) {
            this.visits = new ArrayList<>();
        }
        this.visits.add(fact);
        fact.setEncounter(this);
    }

    /**
     * @return the demographics
     */
    public List<PatientDemographicFact> getDemographics() {
        return demographics;
    }

    /**
     * @return the demographics as a HashMap, indexed by fact short name
     */
    public HashMap<String, PatientDemographicFact> getDemographicsAsHashMap() {
        HashMap<String, PatientDemographicFact> demographicsHM = new HashMap<>();
        demographics.forEach(d -> demographicsHM.put(d.getFactType().getShortName(), d));
        return demographicsHM;
    }

    /**
     * @param demographics the demographics to set
     */
    public void setDemographics(List<PatientDemographicFact> demographics) {
        this.demographics = demographics;
    }

    /**
     * @return the visits
     */
    public List<VisitFact> getVisits() {
        return visits;
    }

    /**
     * @param visits the visits to set
     */
    public void setVisits(List<VisitFact> visits) {
        this.visits = visits;
    }

    @Override
    public String toString() {
        return "Encounter [encounter_id=" + encounterId + ", encounter=" + encounter
                + ", store_datetime=" + this.getStoredFrom() + ", end_datetime=" + this.getValidUntil()
                + ", source_system=" + sourceSystem + ", event_time=" + this.getValidFrom() + "]";
    }

}
