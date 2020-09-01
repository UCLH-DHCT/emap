package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
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
@Table(indexes = {
        @Index(name = "patient_property_fact_index", columnList = "fact", unique = false),
        @Index(name = "patient_property_property_type_index", columnList = "property_type", unique = false),
        @Index(name = "patient_property_stored_from_index", columnList = "storedFrom", unique = false),
        @Index(name = "patient_property_stored_until_index", columnList = "storedUntil", unique = false),
        @Index(name = "patient_property_valid_from_index", columnList = "validFrom", unique = false),
        @Index(name = "patient_property_valid_until_index", columnList = "validUntil", unique = false),
        @Index(name = "patient_property_value_as_datetime_index", columnList = "valueAsDatetime", unique = false),
        @Index(name = "patient_property_value_as_integer_index", columnList = "valueAsInteger", unique = false),
        @Index(name = "patient_property_value_as_link_index", columnList = "valueAsLink", unique = false),
        @Index(name = "patient_property_value_as_real_index", columnList = "valueAsReal", unique = false),
        @Index(name = "patient_property_value_as_string_index", columnList = "valueAsString", unique = false),
        @Index(name = "patient_property_value_as_attribute_index", columnList = "value_as_attribute", unique = false),
})
public class PatientProperty extends Property<PatientFact> implements Serializable {

    private static final long serialVersionUID = 9035602294475836526L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long                    patientPropertyId;

    /**
     * Copy constructor.
     * @param prop property to copy
     */
    public PatientProperty(PatientProperty prop) {
        super(prop);
    }

    /**
     */
    public PatientProperty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PatientProperty invalidateProperty(Instant storedFromUntil, Instant invalidationDate, PatientFact fact) {
        PatientProperty newProp = new PatientProperty(this);
        // stored from/until timestamps are identical so these properties are exactly adjacent in time
        this.setStoredUntil(storedFromUntil);
        newProp.setStoredFrom(storedFromUntil);
        newProp.setValidUntil(invalidationDate);
        // new property needs to be explicitly added to the same fact
        if (fact != null) {
            fact.addProperty(newProp);
        } else {
            getFact().addProperty(newProp);
        }
        return newProp;
    }

    /**
     * @return the propertyId
     */
    public Long getPatientPropertyId() {
        return patientPropertyId;
    }

    /**
     * @param patientPropertyId the propertyId to set
     */
    public void setPatientPropertyId(Long patientPropertyId) {
        this.patientPropertyId = patientPropertyId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Prop " + this.getPropertyType().getShortName() + " = ");
        SortedMap<String, Object> ret = new TreeMap<>();
        // properties can have more than one type filled in
        ret.put("valueAsAttribute", getValueAsAttribute());
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