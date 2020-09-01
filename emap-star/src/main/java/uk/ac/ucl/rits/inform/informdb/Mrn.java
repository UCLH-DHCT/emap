package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@Table(indexes = { @Index(name = "mrnIndex", columnList = "mrn", unique = true) })
@JsonIgnoreProperties("persons")
public class Mrn implements Serializable {

    private static final long  serialVersionUID = -4125275916062604528L;

    /**
     * The MrnId is the UID for the association of an MRN value to a Person.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long            mrnId;

    @OneToMany(targetEntity = MrnEncounter.class, mappedBy = "mrn", cascade = CascadeType.ALL)
    private List<MrnEncounter> encounters;

    @OneToMany(targetEntity = PersonMrn.class, mappedBy = "mrn")
    private List<PersonMrn>    persons          = new ArrayList<>();

    /**
     * The value of the MRN identifier.
     */
    @Column(unique = true, nullable = false)
    private String             mrn;
    private String             sourceSystem;

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant            storedFrom;

    /**
     * @return the mrnId
     */
    public Long getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(Long mrnId) {
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
     * @param enc        the encounter to add
     * @param validFrom  when the association became true
     * @param storedFrom when the association was stored
     */
    public void addEncounter(Encounter enc, Instant validFrom, Instant storedFrom) {
        MrnEncounter mrnEncounter = new MrnEncounter(this, enc);
        mrnEncounter.setValidFrom(validFrom);
        mrnEncounter.setStoredFrom(storedFrom);
        enc.linkMrn(mrnEncounter);
        this.linkEncounter(mrnEncounter);
    }

    /**
     * Add an MrnEncounter to the encounters list.
     *
     * @param mrnEnc The MrnEncouter to add.
     */
    public void linkEncounter(MrnEncounter mrnEnc) {
        if (this.encounters == null) {
            this.encounters = new ArrayList<>();
        }
        this.encounters.add(mrnEnc);
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
        return String.format("Mrn [mrnId=%d, mrn=%s, sourceSystem=%s]", mrnId, mrn, sourceSystem);
    }

    /**
     * @return the Instant this Mrn was first recorded in the database
     */
    public Instant getStoredFrom() {
        return storedFrom;
    }

    /**
     * @param storedFrom the Instant this Mrn was first recorded in the database
     */
    public void setStoredFrom(Instant storedFrom) {
        this.storedFrom = storedFrom;
    }

    /**
     * @return all Persons that are or have ever been associated with this Mrn
     */
    public List<PersonMrn> getPersons() {
        return persons;
    }

    /**
     * Add a backlink to a person.
     *
     * @param p The person mrn relationship.
     */
    public void linkPerson(PersonMrn p) {
        if (this.persons == null) {
            this.persons = new ArrayList<>();
        }
        this.persons.add(p);
    }

    /**
     * Add a Mrn / person association. This will create all the necessary backlinks
     * in the person.
     *
     * @param p          The person to link to.
     * @param validFrom  When this link was created.
     * @param storedFrom When we saved this link to the database.
     */
    public void addPerson(Person p, Instant validFrom, Instant storedFrom) {
        PersonMrn perMrn = new PersonMrn(p, this);
        perMrn.setValidFrom(validFrom);
        perMrn.setStoredFrom(storedFrom);
        p.linkMrn(perMrn);
        this.linkPerson(perMrn);
    }

    /**
     * Apply a function to all currently valid encounters connected to this mrn and
     * collect the results.
     *
     * @param func The function to apply
     * @return List of results
     * @param <R> return type of each function
     */
    public <R> List<R> mapEncounter(Function<Encounter, R> func) {
        List<R> results = new ArrayList<R>();
        for (MrnEncounter me : encounters) {
            if (!me.isValid()) {
                continue;
            }
            Encounter e = me.getEncounter();
            results.add(e.map(func));
        }
        return results;
    }

    /**
     * Apply a function to to this mrn.
     *
     * @param func The function to apply
     * @return The results
     * @param <R> return type of the function
     */
    public <R> R map(Function<Mrn, R> func) {
        return func.apply(this);
    }
}
