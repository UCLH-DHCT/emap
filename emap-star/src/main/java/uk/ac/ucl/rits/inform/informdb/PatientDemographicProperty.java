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
public class PatientDemographicProperty extends TemporalCore implements Property {

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((valueAsAttribute == null) ? 0 : valueAsAttribute.hashCode());
        result = prime * result + ((valueAsBoolean == null) ? 0 : valueAsBoolean.hashCode());
        result = prime * result + ((valueAsDatetime == null) ? 0 : valueAsDatetime.hashCode());
        result = prime * result + ((valueAsInteger == null) ? 0 : valueAsInteger.hashCode());
        result = prime * result + ((valueAsLink == null) ? 0 : valueAsLink.hashCode());
        result = prime * result + ((valueAsReal == null) ? 0 : valueAsReal.hashCode());
        result = prime * result + ((valueAsString == null) ? 0 : valueAsString.hashCode());
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
        PatientDemographicProperty other = (PatientDemographicProperty) obj;
        if (attribute == null) {
            if (other.attribute != null) {
                return false;
            }
        } else if (!attribute.equals(other.attribute)) {
            return false;
        }
        if (valueAsAttribute == null) {
            if (other.valueAsAttribute != null) {
                return false;
            }
        } else if (!valueAsAttribute.equals(other.valueAsAttribute)) {
            return false;
        }
        if (valueAsBoolean == null) {
            if (other.valueAsBoolean != null) {
                return false;
            }
        } else if (!valueAsBoolean.equals(other.valueAsBoolean)) {
            return false;
        }
        if (valueAsDatetime == null) {
            if (other.valueAsDatetime != null) {
                return false;
            }
        } else if (!valueAsDatetime.equals(other.valueAsDatetime)) {
            return false;
        }
        if (valueAsInteger == null) {
            if (other.valueAsInteger != null) {
                return false;
            }
        } else if (!valueAsInteger.equals(other.valueAsInteger)) {
            return false;
        }
        if (valueAsLink == null) {
            if (other.valueAsLink != null) {
                return false;
            }
        } else if (!valueAsLink.equals(other.valueAsLink)) {
            return false;
        }
        if (valueAsReal == null) {
            if (other.valueAsReal != null) {
                return false;
            }
        } else if (!valueAsReal.equals(other.valueAsReal)) {
            return false;
        }
        if (valueAsString == null) {
            if (other.valueAsString != null) {
                return false;
            }
        } else if (!valueAsString.equals(other.valueAsString)) {
            return false;
        }
        return true;
    }
}
