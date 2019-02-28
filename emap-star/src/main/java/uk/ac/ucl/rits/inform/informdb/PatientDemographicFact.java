package uk.ac.ucl.rits.inform.informdb;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * This represents a grouper for a fact about a patient.
 *
 * @author UCL RITS
 *
 */
@Entity
public class PatientDemographicFact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int                              factId;

    @ManyToOne
    @JoinColumn(name = "encounter")
    private Encounter                        encounter;

    private Attribute                        factType;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "fact")
    private List<PatientDemographicProperty> factProperties;

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
    public List<PatientDemographicProperty> getFactProperties() {
        return factProperties;
    }

    /**
     * @param factProperties the factProperties to set
     */
    public void setFactProperties(List<PatientDemographicProperty> factProperties) {
        this.factProperties = factProperties;
    }

}
