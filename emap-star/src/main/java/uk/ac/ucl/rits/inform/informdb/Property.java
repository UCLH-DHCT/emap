package uk.ac.ucl.rits.inform.informdb;

/**
 * Properties in the Fact->Property system must support certain things.
 * @author Jeremy Stein
 *
 */
public interface Property {
    /**
     * @return the attribute (key) for this property
     */
    Attribute getAttribute();
}
