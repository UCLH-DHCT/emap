package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.OldAttribute;

/**
 * Respository to connect to Attributes.
 *
 * @author Jeremy Stein
 */
public interface AttributeRepository extends CrudRepository<OldAttribute, String> {
    /**
     * @param shortName short name of the attribute to look up
     * @return the Attribute object if it exists
     */
    Optional<OldAttribute> findByShortName(String shortName);

    /**
     * @param attributeId the attr id
     * @return Attribute with the given Id
     */
    Optional<OldAttribute> findByAttributeId(long attributeId);

    /**
     * @param shortName list of shortnames
     * @return all Attributes matching one in list
     */
    Set<OldAttribute> findByShortNameIn(SortedSet<String> shortName);

    /**
     * @return all Attributes
     */
    @Override
    Set<OldAttribute> findAll();
}
