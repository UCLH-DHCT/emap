package uk.ac.ucl.rits.inform.pipeline.informdb;

import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.Attribute;

/**
 * Respository to connect to Attributes.
 *
 * @author Jeremy Stein
 */
public interface AttributeRepository extends CrudRepository<Attribute, String> {
    /**
     * @param shortName short name of the attribute to look up
     * @return the Attribute object if it exists
     */
    Optional<Attribute> findByShortName(String shortName);

    /**
     * @param attributeId the attr id
     * @return Attribute with the given Id
     */
    Optional<Attribute> findByAttributeId(long attributeId);

    /**
     * @param shortName list of shortnames
     * @return all Attributes matching one in list
     */
    Set<Attribute> findByShortNameIn(SortedSet<String> shortName);

    /**
     * @return all Attributes
     */
    @Override
    Set<Attribute> findAll();
}
