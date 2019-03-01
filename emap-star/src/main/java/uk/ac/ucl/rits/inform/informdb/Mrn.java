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
@Table(indexes = { @Index(name = "endValidIndex", columnList = "validUntil", unique = false) })
public class Mrn extends TemporalCore {

    /**
     * The MrnId is the UID for the association of an MRN value to a Person.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int    mrnId;

    @ManyToOne
    @JoinColumn()
    private Person person;

    /**
     * The value of the MRN identifier.
     */
    private String mrn;
    private String sourceSystem;

    /**
     * @return the mrnId
     */
    public int getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(int mrnId) {
        this.mrnId = mrnId;
    }

    /**
     * @return the person
     */
    public Person getPerson() {
        return person;
    }

    /**
     * @param person the person to set
     */
    public void setPerson(Person person) {
        this.person = person;
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

    @Override
    public String toString() {
        return "Mrn [mrn_id=" + mrnId + ", person=" + person + ", mrn=" + mrn + ", store_datetime="
                + this.getStoredFrom() + ", end_datetime=" + this.getValidUntil() + ", source_system=" + sourceSystem
                + ", event_time=" + this.getValidFrom() + "]";
    }
}
