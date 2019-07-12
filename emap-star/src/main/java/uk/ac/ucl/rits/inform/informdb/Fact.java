package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.annotations.SortNatural;

/**
 * Handle the common parts of the Fact->Property relationship as this appears in
 * several places.
 *
 * @author Jeremy Stein
 *
 * @param <PropertyType> the type of the Property that this Fact contains
 * @param <F> Self type
 */
@MappedSuperclass
public abstract class Fact<F extends Fact<F, PropertyType>, PropertyType extends Property<F>> extends TemporalCore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long               factId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentFact")
    @SortNatural
    private List<PropertyType> properties;

    @ManyToOne
    @JoinColumn(name = "attributeId")
    private Attribute          factType;

    /**
     * @return the factId
     */
    public Long getFactId() {
        return factId;
    }

    /**
     * @param factId the factId to set
     */
    public void setFactId(Long factId) {
        this.factId = factId;
    }

    /**
     * @param attrKey the attribute
     * @return the property(ies) in this fact with the given attribute (key)
     */
    public List<PropertyType> getPropertyByAttribute(AttributeKeyMap attrKey) {
        return getPropertyByAttribute(attrKey.getShortname(), p -> true);
    }

    /**
     * @param attrKey the attribute
     * @param pred predicate to test prop against
     * @return the property(ies) in this fact with the given attribute (key) and that match pred
     */
    public List<PropertyType> getPropertyByAttribute(AttributeKeyMap attrKey, Predicate<? super PropertyType> pred) {
        return getPropertyByAttribute(attrKey.getShortname(), pred);
    }

    /**
     * @param attrKey the attribute
     * @param pred predicate to test prop against
     * @return the property(ies) in this fact with the given attribute (key) and that match pred
     */
    public List<PropertyType> getPropertyByAttribute(Attribute attrKey, Predicate<? super PropertyType> pred) {
        return getPropertyByAttribute(attrKey.getShortName(), pred);
    }

    /**
     * @param attrKey the attribute
     * @param pred predicate to test prop against
     * @return the property(ies) in this fact with the given attribute (key) and that match pred
     */
    public List<PropertyType> getPropertyByAttribute(String attrKey, Predicate<? super PropertyType> pred) {
        // Might want to cache this as K->[V,V',V'',...] pairs.
        // Many properties have logical constraints on the number of elements that
        // should exist - consider enforcing this here?
        if (properties == null) {
            return new ArrayList<PropertyType>();
        }
        List<PropertyType> propsWithAttr = properties.stream()
                .filter(prop -> prop.getAttribute().getShortName().equals(attrKey) && pred.test(prop)).collect(Collectors.toList());
        return propsWithAttr;
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
     * @return the properties for the fact
     */
    public List<PropertyType> getProperties() {
        return this.properties;
    }

    /**
     * Set the properties for this fact.
     *
     * @param properties The properties to set.
     */
    public void setProperties(List<PropertyType> properties) {
        this.properties = properties;
    }

    /**
     * Invalidate the fact and all its properties.
     *
     * @param invalidationDate the instant at which this fact became invalid
     */
    public void invalidateAll(Instant invalidationDate) {
        setValidUntil(invalidationDate);
        for (PropertyType prop : properties) {
            prop.setValidUntil(invalidationDate);
        }
    }

    /**
     * Add a property to a Fact with backlinks.
     *
     * @param prop A single property to append.
     * @param fact the parent fact
     */
    protected void addProperty(PropertyType prop, F fact) {
        this.linkProperty(prop);
        prop.setParentFact(fact);
    }

    /**
     * Add a property to a Fact with backlinks.
     *
     * @param prop A single property to append.
     */
    public abstract void addProperty(PropertyType prop);

    /**
     * Append a properties to the properties list.
     *
     * @param prop The property to append.
     */
    public void linkProperty(PropertyType prop) {
        if (this.properties == null) {
            this.properties = new ArrayList<>();
        }
        properties.add(prop);
    }

    /**
     * Make a short name to Property map for convenience.
     *
     * @return The map.
     */
    public Map<String, List<PropertyType>> toMap() {
        Map<String, List<PropertyType>> results = new HashMap<String, List<PropertyType>>();
        for (PropertyType p : properties) {
            String key = p.getAttribute().getShortName();
            List<PropertyType> l = results.get(key);
            if (l == null) {
                l = new ArrayList<>();
                results.put(key, l);
            }
            l.add(p);
        }
        return results;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (properties == null ? 0 : properties.hashCode());
        result = prime * result + (factType == null ? 0 : factType.hashCode());
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
        Fact<?, ?> other = (Fact<?, ?>) obj;
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }
        if (factType == null) {
            if (other.factType != null) {
                return false;
            }
        } else if (!factType.equals(other.factType)) {
            return false;
        }
        return true;
    }

}
