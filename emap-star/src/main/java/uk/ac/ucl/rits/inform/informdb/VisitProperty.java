package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A visit property is a single property of a visit.
 *
 * @author UCL RITS
 *
 */
@Entity
public class VisitProperty extends TemporalCore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int       propertyId;

    @ManyToOne
    @JoinColumn(name = "visit")
    private VisitFact visit;

    @ManyToOne
    @JoinColumn(name = "attribute")
    private Attribute attribute;

    private String    valueAsString;
    private int       valueAsInteger;
    private boolean   valueAsBoolean;
    private float     valueAsReal;
    private Instant   valueAsDatetime;

    @ManyToOne
    @JoinColumn(name = "value_as_attribute")
    private Attribute valueAsAttribute;

    private int       valueAsLink;

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
     * @return the visit
     */
    public VisitFact getVisit() {
        return visit;
    }

    /**
     * @param visit the visit to set
     */
    public void setVisit(VisitFact visit) {
        this.visit = visit;
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
