package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.xml.bind.TypeConstraintException;

/**
 * A patient demographic property is a single property of a fact.
 *
 * @author UCL RITS
 *
 */
@Entity
public class PatientDemographicProperty extends TemporalCore {

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
    private Long                   valueAsInteger;
    private Boolean                valueAsBoolean;
    private Double                 valueAsReal;
    private Instant                valueAsDatetime;

    @ManyToOne
    @JoinColumn(name = "value_as_attribute")
    private Attribute              valueAsAttribute;

    private Integer                valueAsLink;

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
     * @param value the value, of any supported type
     */
    public void setValue(Object value) {
        if (value instanceof String) {
            this.valueAsString = (String) value;
        } else if (value instanceof Instant) {
            this.valueAsDatetime = (Instant) value;
        } else if (value instanceof Double) {
            this.valueAsReal = (Double) value;
        } else if (value instanceof Boolean) {
            this.valueAsBoolean = (Boolean) value;
        } else if (value instanceof Attribute) {
            this.valueAsAttribute = (Attribute) value;
        } else {
            throw new TypeConstraintException("Not a supported type: " + value.getClass());
        }
    }

    /**
     * @param valueAsString the valueAsString to set
     */
    public void setValueAsString(String valueAsString) {
        this.valueAsString = valueAsString;
    }

    /**
     * @return the valueAsDatetime
     */
    public Instant getValueAsDatetime() {
        return valueAsDatetime;
    }

    /**
     * @param valueAsDatetime the valueAsDatetime to set
     */
    public void setValueAsDatetime(Instant valueAsDatetime) {
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
     * @return the valueAsInteger
     */
    public Long getValueAsInteger() {
        return valueAsInteger;
    }

    /**
     * @param valueAsInteger the valueAsInteger to set
     */
    public void setValueAsInteger(Long valueAsInteger) {
        this.valueAsInteger = valueAsInteger;
    }

    /**
     * @return the valueAsBoolean
     */
    public Boolean getValueAsBoolean() {
        return valueAsBoolean;
    }

    /**
     * @param valueAsBoolean the valueAsBoolean to set
     */
    public void setValueAsBoolean(Boolean valueAsBoolean) {
        this.valueAsBoolean = valueAsBoolean;
    }

    /**
     * @return the valueAsReal
     */
    public Double getValueAsReal() {
        return valueAsReal;
    }

    /**
     * @param valueAsReal the valueAsReal to set
     */
    public void setValueAsReal(Double valueAsReal) {
        this.valueAsReal = valueAsReal;
    }

    /**
     * @return the valueAsLink
     */
    public Integer getValueAsLink() {
        return valueAsLink;
    }

    /**
     * @param valueAsLink the valueAsLink to set
     */
    public void setValueAsLink(Integer valueAsLink) {
        this.valueAsLink = valueAsLink;
    }

}
