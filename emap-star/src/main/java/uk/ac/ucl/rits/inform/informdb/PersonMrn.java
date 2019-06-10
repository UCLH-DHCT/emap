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
 * One Person can have many MRNs, and one MRN can, over time,
 * be associated with multiple Persons.
 * @author UCL RITS
 *
 */
@Entity
@Table(name = "person_mrn")
@JsonIgnoreProperties("person")
public class PersonMrn extends TemporalCore implements Serializable {

    /**
     * UID for serialisation.
     */
    private static final long serialVersionUID = 7019692664925413320L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "person", nullable = false)
    private Person person;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "mrn", nullable = false)
    private Mrn mrn;

    /**
     * .
     */
    public PersonMrn() {
    }

    /**
     * create a new person/mrn association.
     * @param person the person
     * @param mrn the mrn
     */
    public PersonMrn(Person person, Mrn mrn) {
        this.person = person;
        this.mrn = mrn;
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
}
