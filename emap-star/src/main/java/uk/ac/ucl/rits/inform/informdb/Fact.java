package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
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
    /**
     */
    public Fact() {
    }

    /**
     * Copy constructor. Cloned facts do not inherit the parent and child facts nor properties.
     * @param other object to copy from
     */
    public Fact(Fact<F, PropertyType> other) {
        super(other);
        factType = other.factType;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "fact")
    @SortNatural
    private List<PropertyType> properties;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentFact")
    @SortNatural
    protected final List<F>    childFacts = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "fact_type", nullable = false)
    private Attribute          factType;

    @JoinColumn(name = "parent_fact")
    @ManyToOne(cascade = CascadeType.ALL)
    private F                  parentFact;

    /**
     * @return the factId
     */
    public abstract Long getFactId();

    /**
     * @param factId the factId to set
     */
    public abstract void setFactId(Long factId);

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
                .filter(prop -> prop.getPropertyType().getShortName().equals(attrKey) && pred.test(prop))
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
     * @param storedFromUntil  the time the DB was updated to reflect this fact
     *                         becoming invalid
     * @param invalidationDate the instant at which this fact became invalid
     */
    public void invalidateAll(Instant storedFromUntil, Instant invalidationDate) {
        F newFact = (F) invalidateFact(storedFromUntil, invalidationDate);
        for (PropertyType prop : properties) {
            prop.invalidateProperty(storedFromUntil, invalidationDate, newFact);
        }
        if (this.getParentFact() != null) {
            this.getParentFact().addChildFact(newFact);
        }
    }

    /**
     * Invalid a fact (but not its properties). The fact will be deleted but not
     * invalidated and then a new, invalidated fact will be created in its place
     * which shows the validity interval. If a new, current fact is required as a
     * replacement this must be created separately.
     *
     * @param storedFromUntil when does the change happen in the DB
     * @param invalidationDate when did the fact stop being true
     * @return the newly created fact that has been invalidated (but not deleted)
     */
    public abstract Fact<F, PropertyType> invalidateFact(Instant storedFromUntil, Instant invalidationDate);

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
        prop.setFact(fact);
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
            String key = p.getPropertyType().getShortName();
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

    /**
     * Test equality of properties, optionally ignoring one or more by property type.
     * @param other               the other fact to test property equality to
     * @param attributesToIgnore  attributes to ignore when determining equality
     * @return true iff all properties that you care about are equal, ignoring order of properties
     */
    protected boolean certainPropertiesEqual(Fact<F, PropertyType> other, List<AttributeKeyMap> attributesToIgnore) {
        // make a shallow, filtered copy of the properties that we care about when deciding equality
        List<PropertyType> thisProperties = this.properties.stream()
                .filter(p -> !attributesToIgnore.stream().anyMatch(a -> p.isOfType(a))).collect(Collectors.toList());
        List<PropertyType> otherProperties = other.properties.stream()
                .filter(p -> !attributesToIgnore.stream().anyMatch(a -> p.isOfType(a))).collect(Collectors.toList());
        if (thisProperties == null) {
            if (otherProperties != null) {
                return false;
            }
        } else if (thisProperties.size() != otherProperties.size()) {
            return false;
        } else if (!thisProperties.containsAll(otherProperties)) {
            // same elements in different order counts as equal
            return false;
        }
        return true;
    }

    /**
     * Test equality of facts, optionally ignoring one or more by properties by type.
     * @param obj                 the other fact to test equality to
     * @param attributesToIgnore  attributes to ignore when determining equality of properties
     * @return true iff facts are equal, and all properties that you care about are equal, ignoring order of properties
     */
    protected boolean equalsIgnoringProperties(Object obj, List<AttributeKeyMap> attributesToIgnore) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Fact<F, PropertyType> other = (Fact<F, PropertyType>) obj;
        if (factType == null) {
            if (other.factType != null) {
                return false;
            }
        } else if (!factType.equals(other.factType)) {
            return false;
        }
        if (!certainPropertiesEqual(other, attributesToIgnore)) {
            return false;
        }
        return true;
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
        if (factType == null) {
            if (other.factType != null) {
                return false;
            }
        } else if (!factType.equals(other.factType)) {
            return false;
        }
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
