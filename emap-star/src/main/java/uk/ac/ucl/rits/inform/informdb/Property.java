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
    @JoinColumn(name = "property_type")
    private Attribute propertyType;

    @JoinColumn(name = "fact")
    @ManyToOne
    private F fact;

    @Column(columnDefinition = "text")
    private String    valueAsString;
    private Long      valueAsInteger;
    private Double    valueAsReal;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant   valueAsDatetime;

    @ManyToOne
    @JoinColumn(name = "value_as_attribute")
    private Attribute valueAsAttribute;

    private Long      valueAsLink;

    /**
     */
    public Property() {
    }

    /**
     * Copy constructor.
     * @param prop object to copy from
     */
    public Property(Property<F> prop) {
        super(prop);
        propertyType = prop.propertyType;
        fact = prop.fact;
        valueAsString = prop.valueAsString;
        valueAsInteger = prop.valueAsInteger;
        valueAsReal = prop.valueAsReal;
        valueAsDatetime = prop.valueAsDatetime;
        valueAsAttribute = prop.valueAsAttribute;
        valueAsLink = prop.valueAsLink;
    }

    /**
     * @return the fact this property belongs to (property grouper)
     */
    public F getFact() {
        return fact;
    }

    /**
     * @param fact the parentFact to set
     */
    public void setFact(F fact) {
        this.fact = fact;
    }

    /**
     * @return the propertyType
     */
    public Attribute getPropertyType() {
        return propertyType;
    }

    /**
     * @param propertyType the property type attribute to set
     */
    public void setPropertyType(Attribute propertyType) {
        this.propertyType = propertyType;
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
        return propertyType.getShortName().compareTo(o.propertyType.getShortName());
    }

    /**
     * Get value in a generic way.
     * @param <T> the value type
     * @param type the type you want to return
     * @return the value of the specified type
     */
    public <T> Object getValue(Class<T> type) {
        if (type.equals(String.class)) {
            return this.valueAsString;
        } else if (type.equals(Instant.class)) {
            return this.valueAsDatetime;
        } else if (type.equals(Double.class)) {
            return this.valueAsReal;
        } else if (type.equals(Attribute.class)) {
            return this.valueAsAttribute;
        } else {
            throw new TypeConstraintException("Not a supported type: " + type);
        }
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
    public void setValue(Attribute value) {
        this.valueAsAttribute = value;
    }

    /**
     * Invalidate this property without updating any column except for stored_until.
     * Do not compare property values to check for changes. Deletes the existing row
     * by setting stored_until and creates a new row showing the updated validity
     * interval for the property.
     *
     * @param storedFromUntil  When did the change to the DB occur? This will be a
     *                         time very close to the present moment. Will be used
     *                         as a storedFrom and/or a storedUntil as appropriate.
     * @param invalidationDate When did the change to the DB become true. This is
     *                         the actual patient event took place so can be
     *                         significantly in the past.
     * @param fact             the fact to add newly created invalid properties to,
     *                         or null to add these to the same fact the
     *                         pre-existing properties were part of
     * @return the newly created row showing the new state of this property
     */
    public abstract Property<F> invalidateProperty(Instant storedFromUntil, Instant invalidationDate, F fact);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (propertyType == null ? 0 : propertyType.hashCode());
        result = prime * result + (valueAsAttribute == null ? 0 : valueAsAttribute.hashCode());
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
        if (propertyType == null) {
            if (other.propertyType != null) {
                return false;
            }
        } else if (!propertyType.equals(other.propertyType)) {
            return false;
        }
        if (valueAsAttribute == null) {
            if (other.valueAsAttribute != null) {
                return false;
            }
        } else if (!valueAsAttribute.equals(other.valueAsAttribute)) {
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
