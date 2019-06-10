package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@Table(indexes = { @Index(name = "encounterIndex", columnList = "encounter", unique = false) })
@JsonIgnoreProperties("mrns")
public class Encounter implements Serializable {

    private static final long serialVersionUID = -6495238097074592105L;

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
    private List<MrnEncounter>           mrns;

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
     * Add a patient demographic fact to this encounter. Creating back links in the
     * fact.
     *
     * @param fact The fact to add
     */
    public void addDemographic(PatientDemographicFact fact) {
        this.linkDemographic(fact);
        fact.setEncounter(this);
    }

    /**
     * Add a demographic fact to the demographics.
     *
     * @param fact The fact to add.
     */
    public void linkDemographic(PatientDemographicFact fact) {
        if (this.demographics == null) {
            this.demographics = new ArrayList<>();
        }
        this.demographics.add(fact);
    }

    /**
     * Add a visit fact to this encounter.
     *
     * @param fact The fact to add
     */
    public void addVisit(VisitFact fact) {
        this.linkVisit(fact);
        fact.setEncounter(this);
    }

    /**
     * Add a visit fact to the visits array.
     *
     * @param fact The VisitFact to add.
     */
    public void linkVisit(VisitFact fact) {
        if (this.visits == null) {
            this.visits = new ArrayList<>();
        }
        this.visits.add(fact);
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
    public Map<String, PatientDemographicFact> getDemographicsAsHashMap() {
        Map<String, PatientDemographicFact> demographicsHM = new HashMap<>();
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

    /**
     * Add a Mrn Encounter relationship to this encounter and link it back to the
     * MRN.
     *
     * @param mrn        The Mrn to link to
     * @param validFrom  The time this relationship came into effect
     * @param storedFrom The time we stored this
     */
    public void addMrn(Mrn mrn, Instant validFrom, Instant storedFrom) {
        MrnEncounter mrnEncounter = new MrnEncounter(mrn, this);
        mrnEncounter.setValidFrom(validFrom);
        mrnEncounter.setStoredFrom(storedFrom);
        mrn.linkEncounter(mrnEncounter);
        this.linkMrn(mrnEncounter);
    }

    /**
     * Add an MrnEncounter to the mrns list.
     *
     * @param mrnEnc The MrnEncounter to add.
     */
    public void linkMrn(MrnEncounter mrnEnc) {
        if (this.mrns == null) {
            this.mrns = new ArrayList<>();
        }
        this.mrns.add(mrnEnc);
    }

    @Override
    public String toString() {
        return String.format("Encounter [encounter_id=%d, encounter=%s, source_system=%s]", encounterId, encounter,
                sourceSystem);
    }

}
