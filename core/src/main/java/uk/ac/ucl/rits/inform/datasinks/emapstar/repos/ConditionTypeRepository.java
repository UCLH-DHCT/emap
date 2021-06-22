package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;

import java.util.Optional;

/**
 * ConditionType repository.
 * @author Stef Piatek
 */
public interface ConditionTypeRepository extends CrudRepository<ConditionType, Long> {
    /**
     * @param dataType Type of data (patient infection or problem list)
     * @param name     name of condition
     * @return possible patient condition type
     */
    Optional<ConditionType> findByDataTypeAndName(String dataType, String name);
}
