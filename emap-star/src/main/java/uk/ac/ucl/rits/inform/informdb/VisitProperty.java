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
 * A visit property is a single property of a visit.
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties("visit")
public class VisitProperty extends Property<VisitFact> implements Serializable {

    private static final long serialVersionUID = 6078510171794243662L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int       propertyId;

    @JoinColumn(name = "parent_fact")
    @ManyToOne()
    private VisitFact parentFact;

    /**
     * @return the parentFact
     */
    @Override
    public VisitFact getParentFact() {
        return parentFact;
    }

    /**
     * @param parentFact the parentFact to set
     */
    @Override
    public void setParentFact(VisitFact parentFact) {
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
