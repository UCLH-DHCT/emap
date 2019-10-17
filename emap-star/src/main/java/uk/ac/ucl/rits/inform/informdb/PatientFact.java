package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This represents a grouper for a fact about a patient.
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties({"encounter", "valid", "childFacts", "childFactsAsMap", "parentFact"})
@Table(indexes = {
        @Index(name = "parent_fact_index", columnList = "parent_fact", unique = false),
        @Index(name = "encounter_index", columnList = "encounter", unique = false),
})
public class PatientFact extends Fact<PatientFact, PatientProperty> implements Serializable {

    private static final long serialVersionUID = -5867434510066589366L;

    @ManyToOne
    @JoinColumn(name = "encounter", referencedColumnName = "encounter")
    private Encounter         encounter;

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

    @Override
    public void addProperty(PatientProperty prop) {
        super.addProperty(prop, this);
    }

    @Override
    public void addChildFact(PatientFact fact) {
        childFacts.add(fact);
        fact.setParentFact(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" factid = " + getFactId() + " ");
        sb.append(this.getFactType().getShortName());
        sb.append("[id=" + getFactType().getAttributeId() + "]");
        sb.append(" --- ");
        PatientFact parentFact = getParentFact();
        String factId;
        if (parentFact == null) {
            factId = "null";
        } else {
            factId = parentFact.getFactId().toString();
        }
        sb.append(" parent factid = " + factId + " ");
        sb.append(" --- ");
        for (PatientProperty p : getProperties()) {
            sb.append("  " + p.toString() + ",  ");
        }
        return sb.toString();
    }

}
