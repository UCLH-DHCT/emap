package uk.ac.ucl.rits.inform.pipeline.informdb;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

/**
 * A Repository to handle PatientFacts.
 *
 * @author Jeremy Stein
 */
public interface PatientFactRepository extends CrudRepository<PatientFact, Long> {
    /**
     * @param propertyAttribute the attribute short name (key) that must match
     * @param propertyValue the string value that must match
     * @return all currently valid PatientFact objects containing a property with the given property key and string value
     */
    @Query("SELECT pf FROM PatientFact pf "
            + " INNER JOIN pf.properties AS pp "
            + " INNER JOIN pp.attribute AS attr "
            + " WHERE attr.shortName=?1 AND pp.valueAsString=?2 AND pf.validUntil is null ")
    List<PatientFact> findAllWithProperty(String propertyAttribute, String propertyValue);

    /**
     * Find all currently valid pathology orders with the given order number.
     * @param orderNumber the order number to search for
     * @return matching pathology orders as PatientFacts
     */
    default List<PatientFact> findAllPathologyOrdersByOrderNumber(String orderNumber) {
        return findAllWithProperty(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER.getShortname(), orderNumber);
    }
}
