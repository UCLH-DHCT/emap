package uk.ac.ucl.rits.inform.informdb;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * One Person can have many MRNs, and one MRN can, over time,
 * be associated with multiple Persons.
 * @author UCL RITS
 *
 */
@Entity
@Table(name = "person_mrn")
public class PersonMrn extends TemporalCore {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "person") //, nullable = false)
    private Person person;

    @ManyToOne
    @JoinColumn(name = "mrn") //, nullable = false)
    private Mrn mrn;

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

//    public void setPerson(Person person) {
//        this.person = person;
//    }

    /**
     * @return the MRN in the association
     */
    public Mrn getMrn() {
        return mrn;
    }

//    public void setMrn(Mrn mrn) {
//        this.mrn = mrn;
//    }

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
