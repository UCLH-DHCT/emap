package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A patient demographic property is a single property of a fact.
 *
 * @author UCL RITS
 *
 */
@Entity
public class PatientDemographicProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int                    propertyId;

    @ManyToOne
    @JoinColumn(name = "fact")
    private PatientDemographicFact fact;

    @ManyToOne
    @JoinColumn(name = "attribute")
    private Attribute              attribute;

    private String                 valueAsString;
    private int                    valueAsInteger;
    private boolean                valueAsBoolean;
    private float                  valueAsReal;
    private Timestamp              valueAsDatetime;

    @ManyToOne
    @JoinColumn(name = "value_as_attribute")
    private Attribute              valueAsAttribute;

    private int                    valueAsLink;


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

    /**
     * @return the fact
     */
    public PatientDemographicFact getFact() {
        return fact;
    }

    /**
     * @param fact the fact to set
     */
    public void setFact(PatientDemographicFact fact) {
        this.fact = fact;
    }

    /**
     * @return the attribute
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * @param attribute the attribute to set
     */
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * @return the valueAsString
     */
    public String getValueAsString() {
        return valueAsString;
    }

    /**
     * @param valueAsString the valueAsString to set
     */
    public void setValueAsString(String valueAsString) {
        this.valueAsString = valueAsString;
    }

    /**
     * @return the valueAsInteger
     */
    public int getValueAsInteger() {
        return valueAsInteger;
    }

    /**
     * @param valueAsInteger the valueAsInteger to set
     */
    public void setValueAsInteger(int valueAsInteger) {
        this.valueAsInteger = valueAsInteger;
    }

    /**
     * @return the valueAsBoolean
     */
    public boolean isValueAsBoolean() {
        return valueAsBoolean;
    }

    /**
     * @param valueAsBoolean the valueAsBoolean to set
     */
    public void setValueAsBoolean(boolean valueAsBoolean) {
        this.valueAsBoolean = valueAsBoolean;
    }

    /**
     * @return the valueAsReal
     */
    public float getValueAsReal() {
        return valueAsReal;
    }

    /**
     * @param valueAsReal the valueAsReal to set
     */
    public void setValueAsReal(float valueAsReal) {
        this.valueAsReal = valueAsReal;
    }

    /**
     * @return the valueAsDatetime
     */
    public Timestamp getValueAsDatetime() {
        return valueAsDatetime;
    }

    /**
     * @param valueAsDatetime the valueAsDatetime to set
     */
    public void setValueAsDatetime(Timestamp valueAsDatetime) {
        this.valueAsDatetime = valueAsDatetime;
    }

    /**
     * @return the valueAsAttribute
     */
    public Attribute getValueAsAttribute() {
        return valueAsAttribute;
    }

    /**
     * @param valueAsAttribute the valueAsAttribute to set
     */
    public void setValueAsAttribute(Attribute valueAsAttribute) {
        this.valueAsAttribute = valueAsAttribute;
    }

    /**
     * @return the valueAsLink
     */
    public int getValueAsLink() {
        return valueAsLink;
    }

    /**
     * @param valueAsLink the valueAsLink to set
     */
    public void setValueAsLink(int valueAsLink) {
        this.valueAsLink = valueAsLink;
    }

}
