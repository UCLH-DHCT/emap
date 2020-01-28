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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentFact")
    @SortNatural
    protected final List<F>    childFacts = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "fact_type")
    private Attribute          factType;

    @JoinColumn(name = "parent_fact")
    @ManyToOne(cascade = CascadeType.ALL)
    private F                  parentFact;

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
     * @param pred    predicate to test prop against
     * @return the property(ies) in this fact with the given attribute (key) and
     *         that match pred. Can be empty list.
     */
    public List<PropertyType> getPropertyByAttribute(AttributeKeyMap attrKey, Predicate<? super PropertyType> pred) {
        return getPropertyByAttribute(attrKey.getShortname(), pred);
    }

    /**
     * @param attrKey the attribute
     * @param pred    predicate to test prop against
     * @return the property(ies) in this fact with the given attribute (key) and
     *         that match pred
     */
    public List<PropertyType> getPropertyByAttribute(Attribute attrKey, Predicate<? super PropertyType> pred) {
        return getPropertyByAttribute(attrKey.getShortName(), pred);
    }

    /**
     * @param attrKey the attribute
     * @param pred    predicate to test prop against
     * @return the property(ies) in this fact with the given attribute (key) and
     *         that match pred
     */
    public List<PropertyType> getPropertyByAttribute(String attrKey, Predicate<? super PropertyType> pred) {
        // Might want to cache this as K->[V,V',V'',...] pairs.
        // Many properties have logical constraints on the number of elements that
        // should exist - consider enforcing this here?
        if (properties == null) {
            return new ArrayList<PropertyType>();
        }
        List<PropertyType> propsWithAttr = properties.stream()
                .filter(prop -> prop.getAttribute().getShortName().equals(attrKey) && pred.test(prop))
                .collect(Collectors.toList());
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
     * @param attrKM the attribute enum to check against
     * @return whether this Fact is of the given fact type
     */
    public boolean isOfType(AttributeKeyMap attrKM) {
        return factType.getShortName().equals(attrKM.getShortname());
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
     * Recursively de-nullify validFrom values for all properties of this fact and
     * descendant facts. Preferentially use the validFrom from the root fact, otherwise
     * use the supplied alternative value.
     *
     * @param validFrom The alternative validFrom value to cascade downwards if this fact has a null value.
     *                  Supply null if this fact's validFrom is non-null and you want to use it.
     */
    public void cascadeValidFrom(Instant validFrom) {
        // Favour the current fact's validFrom if it exists,
        // otherwise use the one cascaded from above.
        if (this.getValidFrom() != null) {
            validFrom = this.getValidFrom();
        } else {
            this.setValidFrom(validFrom);
        }
        for (PropertyType prop : properties) {
            if (prop.getValidFrom() == null) {
                prop.setValidFrom(validFrom);
            }
        }
        for (F child : childFacts) {
            child.cascadeValidFrom(validFrom);
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
        } else if (properties.size() != other.properties.size()) {
            return false;
        } else if (!properties.containsAll(other.properties)) {
            // same elements in different order counts as equal
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

    /**
     * Facts, as well as having properties, can have other child facts. Add them
     * here.
     *
     * @param fact the child fact to add
     */
    public abstract void addChildFact(F fact);

    /**
     * @return the child facts of this fact
     */
    public List<F> getChildFacts() {
        return childFacts;
    }

    /**
     * @return The parent fact of this fact
     */
    public F getParentFact() {
        return parentFact;
    }

    /**
     * Set this fact's parent fact.
     *
     * @param parentFact the parent fact
     */
    public void setParentFact(F parentFact) {
        this.parentFact = parentFact;
    }

    /**
     * Get the id of the parent fact.
     * Returns null if there isn't a parent fact, or it doesn't have an id.
     *
     * @return Id of parent fact or null.
     */
    public Long getParentFactId() {
        if (this.parentFact == null) {
            return null;
        }
        return this.parentFact.getFactId();
    }
}
