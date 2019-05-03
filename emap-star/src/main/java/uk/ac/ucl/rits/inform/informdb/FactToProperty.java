package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handle the common parts of the Fact->Property relationship
 * as this appears in several places.
 * @author jeremystein
 *
 * @param <PropertyType> the type of the Property that this Fact contains
 */
public interface FactToProperty<PropertyType extends Property> {

    /**
     * @param attrKey the attribute
     * @return the property(ies) in this fact with the given attribute (key)
     */
    default List<PropertyType> getPropertyByAttribute(AttributeKeyMap attrKey) {
        return getPropertyByAttribute(attrKey.getShortname());
    }

    /**
     * @param attrKey the attribute
     * @return the property(ies) in this fact with the given attribute (key)
     */
    default List<PropertyType> getPropertyByAttribute(Attribute attrKey) {
        return getPropertyByAttribute(attrKey.getShortName());
    }

    /**
     * @param attrKey the attribute
     * @return the property(ies) in this fact with the given attribute (key)
     */
    default List<PropertyType> getPropertyByAttribute(String attrKey) {
        // Might want to cache this as K->[V,V',V'',...] pairs.
        // Many properties have logical constraints on the number of elements that
        // should exist - consider enforcing this here?
        List<PropertyType> props = getFactProperties();
        if (props == null) {
            return new ArrayList<PropertyType>();
        }
        List<PropertyType> propsWithAttr = props.stream()
                .filter(prop -> prop.getAttribute().getShortName().equals(attrKey))
                .collect(Collectors.toList());
        return propsWithAttr;
    }

    /**
     * @return the properties for the fact
     */
    List<PropertyType> getFactProperties();

    /**
     * Invalidate the fact and all its properties.
     * @param invalidationDate the instant at which this fact became invalid
     */
    void invalidateAll(Instant invalidationDate);
}
