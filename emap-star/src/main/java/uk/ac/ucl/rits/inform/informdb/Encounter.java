package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
@JsonIgnoreProperties({ "mrns", "factsAsMap" })
public class Encounter implements Serializable {

    private static final long serialVersionUID = -6495238097074592105L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int               encounterId;

    @Column(unique = false, nullable = false)
    private String            encounter;
    private String            sourceSystem;

    @ManyToOne(targetEntity = Encounter.class)
    @JoinColumn(name = "parent_encounter")
    private Encounter         parentEncounter;

    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL)
    private Set<PatientFact>  facts;

    @OneToMany(targetEntity = MrnEncounter.class, mappedBy = "encounter")
    private Set<MrnEncounter> mrns;

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
     * Add a patient fact to this encounter. Creating back links in the fact.
     *
     * @param fact The fact to add
     */
    public void addFact(PatientFact fact) {
        this.linkFact(fact);
        fact.setEncounter(this);
    }

    /**
     * Add a patient fact to the demographics.
     *
     * @param fact The fact to add.
     */
    public void linkFact(PatientFact fact) {
        if (this.facts == null) {
            this.facts = new HashSet<>();
        }
        this.facts.add(fact);
    }

    /**
     * @return the facts
     */
    public Set<PatientFact> getFacts() {
        return facts;
    }

    /**
     * @return the facts as a Map, indexed by fact short name
     */
    public Map<String, PatientFact> getFactsAsMap() {
        Map<String, PatientFact> map = new HashMap<>();
        facts.forEach(d -> map.put(d.getFactType().getShortName(), d));
        return map;
    }

    /**
     * @param facts the facts to set
     */
    public void setFacts(Set<PatientFact> facts) {
        this.facts = facts;
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
            this.mrns = new HashSet<>();
        }
        this.mrns.add(mrnEnc);
    }

    @Override
    public String toString() {
        return String.format("Encounter [encounter_id=%d, encounter=%s, source_system=%s]", encounterId, encounter,
                sourceSystem);
    }

    /**
     * Apply a function on this encounter and get a result.
     *
     * @param func The function to apply
     * @return The result of the function
     * @param <R> The return type of the function
     */
    public <R> R map(Function<Encounter, R> func) {
        return func.apply(this);
    }

}
