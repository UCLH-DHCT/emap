package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

/**
 * This represents a grouper for a fact about a patient.
 *
 * @author UCL RITS
 *
 */
@Entity
public class PatientDemographicFact extends TemporalCore implements FactToProperty<PatientDemographicProperty> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int                              factId;

    @ManyToOne
    @JoinColumn(name = "encounter", referencedColumnName = "encounter")
    private Encounter                        encounter;

    @ManyToOne
    @JoinColumn(name = "attributeId")
    private Attribute                        factType;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "fact")
    @Sort(type = SortType.NATURAL)
    private SortedSet<PatientDemographicProperty> factProperties;

    /**
     * @return the factId
     */
    public int getFactId() {
        return factId;
    }

    /**
     * @param factId the factId to set
     */
    public void setFactId(int factId) {
        this.factId = factId;
    }

    /**
     * @return the encounter
     */
    public Encounter getEncounter() {
        return encounter;
    }

    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    /**
     * @return the factType
     */
    public Attribute getFactType() {
        return factType;
    }

    /**
     * @param factType the factType to set
     */
    public void setFactType(Attribute factType) {
        this.factType = factType;
    }

    /**
     * @return the factProperties
     */
    public SortedSet<PatientDemographicProperty> getFactProperties() {
        return factProperties;
    }

    /**
     * @param factProperties the factProperties to set
     */
    public void setFactProperties(SortedSet<PatientDemographicProperty> factProperties) {
        this.factProperties = factProperties;
    }

    /**
     * Add a property to a fact.
     *
     * @param prop A single property to append.
     */
    public void addProperty(PatientDemographicProperty prop) {
        if (this.factProperties == null) {
            this.factProperties = new TreeSet<>();
        }
        this.factProperties.add(prop);
        prop.setFact(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((factProperties == null) ? 0 : factProperties.hashCode());
        result = prime * result + ((factType == null) ? 0 : factType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PatientDemographicFact other = (PatientDemographicFact) obj;
        if (factProperties == null) {
            if (other.factProperties != null) {
                return false;
            }
        } else if (!factProperties.equals(other.factProperties)) {
            return false;
        }
        if (factType == null) {
            if (other.factType != null) {
                return false;
            }
        } else if (!factType.equals(other.factType)) {
            return false;
        }
        return true;
    }

    @Override
    public void invalidateAll(Instant invalidationDate) {
        setValidUntil(invalidationDate);
        for (PatientDemographicProperty pdp: getFactProperties()) {
            pdp.setValidUntil(invalidationDate);
        }
    }

}
