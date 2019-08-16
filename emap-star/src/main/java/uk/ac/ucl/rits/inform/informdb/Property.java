package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.TypeConstraintException;

/**
 * Properties in the Fact->Property system must support certain things.
 *
 * @author Jeremy Stein
 *
 * @param <F> Parent fact type
 *
 */
@MappedSuperclass
public abstract class Property<F extends Fact<F, ?>> extends TemporalCore implements Comparable<Property<?>> {

    @ManyToOne
    @JoinColumn(name = "attribute")
    private Attribute attribute;

    @Column(columnDefinition = "text")
    private String    valueAsString;
    private Long      valueAsInteger;
    private Boolean   valueAsBoolean;
    private Double    valueAsReal;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant   valueAsDatetime;

    @ManyToOne
    @JoinColumn(name = "value_as_attribute")
    private Attribute valueAsAttribute;

    private Long      valueAsLink;

    /**
     * @return the parentFact
     */
    public abstract F getParentFact();

    /**
     * @param fact the parentFact to set
     */
    public abstract void setParentFact(F fact);

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
     * @return the valueAsLink
     */
    public Long getValueAsLink() {
        return valueAsLink;
    }

    /**
     * @param valueAsLink the valueAsLink to set
     */
    public void setValueAsLink(Long valueAsLink) {
        this.valueAsLink = valueAsLink;
    }

    @Override
    public int compareTo(Property<?> o) {
        return attribute.getShortName().compareTo(o.attribute.getShortName());
    }

    /**
     * Helper method to set the correct value.
     *
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
     * Set value helper.
     *
     * @param value The value to set.
     */
    public void setValue(String value) {
        this.valueAsString = value;
    }

    /**
     * Set value helper.
     *
     * @param value The value to set.
     */
    public void setValue(Instant value) {
        this.valueAsDatetime = value;
    }

    /**
     * Set value helper.
     *
     * @param value The value to set.
     */
    public void setValue(double value) {
        this.valueAsReal = value;
    }

    /**
     * Set value helper.
     *
     * @param value The value to set.
     */
    public void setValue(boolean value) {
        this.valueAsBoolean = value;
    }

    /**
     * Set value helper.
     *
     * @param value The value to set.
     */
    public void setValue(Attribute value) {
        this.valueAsAttribute = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (attribute == null ? 0 : attribute.hashCode());
        result = prime * result + (valueAsAttribute == null ? 0 : valueAsAttribute.hashCode());
        result = prime * result + (valueAsBoolean == null ? 0 : valueAsBoolean.hashCode());
        result = prime * result + (valueAsDatetime == null ? 0 : valueAsDatetime.hashCode());
        result = prime * result + (valueAsInteger == null ? 0 : valueAsInteger.hashCode());
        result = prime * result + (valueAsLink == null ? 0 : valueAsLink.hashCode());
        result = prime * result + (valueAsReal == null ? 0 : valueAsReal.hashCode());
        result = prime * result + (valueAsString == null ? 0 : valueAsString.hashCode());
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
        Property<?> other = (Property<?>) obj;
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
