package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One Person can have many MRNs, and one MRN can, over time, be associated with
 * multiple Persons. Each PersonMrn represents a single instance of a
 * relationship between a given person id and a specific Mrn. These change over
 * time as new people, mrns and merges occur.
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(name = "person_mrn",
        indexes = { @Index(columnList = "mrn", unique = false), @Index(columnList = "person", unique = false), })
@JsonIgnoreProperties({ "person", "valid" })
public class PersonMrn extends TemporalCore implements Serializable {

    private static final long serialVersionUID = 7019692664925413320L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int               id;

    @ManyToOne
    @JoinColumn(name = "person", nullable = false)
    private Person            person;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "mrn", nullable = false)
    private Mrn               mrn;

    @Column(nullable = false)
    private Boolean           live;

    /**
     * Create a new person/mrn association.
     */
    public PersonMrn() {}

    /**
     * Create a new person/mrn association.
     *
     * @param person the person
     * @param mrn    the mrn
     */
    public PersonMrn(Person person, Mrn mrn) {
        this.person = person;
        this.mrn = mrn;
        this.live = true;
    }

    /**
     * @return the Person in the association
     */
    public Person getPerson() {
        return person;
    }

    /**
     * Set the person.
     *
     * @param person The new person.
     */
    public void setPerson(Person person) {
        this.person = person;
    }

    /**
     * @return the MRN in the association
     */
    public Mrn getMrn() {
        return mrn;
    }

    /**
     * Set the Mrn.
     *
     * @param mrn The mrn
     */
    public void setMrn(Mrn mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the Id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the Id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the live
     */
    public Boolean isLive() {
        return live;
    }

    /**
     * @param live the live to set
     */
    public void setLive(Boolean live) {
        this.live = live;
    }

    @Override
    public String toString() {
        return "PersonMrn [id=" + id + ", person=" + person.getPersonId() + ", mrn=" + mrn + ", validUntil="
                + getValidUntil() + ", live=" + live + "]";
    }

}
