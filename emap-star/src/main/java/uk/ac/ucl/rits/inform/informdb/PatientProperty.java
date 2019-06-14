package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A patient demographic property is a single property of a fact.
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties("fact")
public class PatientProperty extends Property<PatientFact> implements Serializable {

    private static final long serialVersionUID = 9035602294475836526L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int                    propertyId;

    @JoinColumn(name = "parent_fact")
    @ManyToOne
    private PatientFact parentFact;

    /**
     * @return the parentFact
     */
    @Override
    public PatientFact getParentFact() {
        return parentFact;
    }

    /**
     * @param parentFact the parentFact to set
     */
    @Override
    public void setParentFact(PatientFact parentFact) {
        this.parentFact = parentFact;
    }

    /**
     * @return the propertyId
     */
    public int getPropertyId() {
        return propertyId;
    }

    /**
     * @param propertyId the propertyId to set
     */
    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
    }

}
