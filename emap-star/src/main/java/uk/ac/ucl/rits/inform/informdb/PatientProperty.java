package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

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
 * A patient demographic property is a single property of a fact.
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties({"parentFact", "valid"})
@Table(indexes = { @Index(name = "patient_property_parent_fact_index", columnList = "parent_fact", unique = false),
        @Index(name = "patient_property_string_index", columnList = "valueAsString", unique = false),
        @Index(name = "patient_property_attribute_index", columnList = "attribute", unique = false) })
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Prop " + this.getAttribute().getShortName() + " = ");
        SortedMap<String, Object> ret = new TreeMap<>();
        // properties can have more than one type filled in
        ret.put("valueAsAttribute", getValueAsAttribute());
        ret.put("valueAsBoolean", getValueAsBoolean());
        ret.put("valueAsDatetime", getValueAsDatetime());
        ret.put("valueAsInteger", getValueAsInteger());
        ret.put("valueAsLink", getValueAsLink());
        ret.put("valueAsReal", getValueAsReal());
        ret.put("valueAsString", getValueAsString());

        for (Entry<String, Object> foo : ret.entrySet()) {
            if (foo.getValue() != null) {
                sb.append("[" + foo.getKey() + "]" + foo.getValue() + " ");
            }
        }

        return sb.toString();
    }

}
