package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;

import java.util.List;
import java.util.Optional;

/**
 * ConditionType repository.
 * @author Stef Piatek
 */
public interface ConditionTypeRepository extends CrudRepository<ConditionType, Long> {
    /**
     * @param dataType Type of data (patient infection or problem list)
     * @param code     EPIC code for condition
     * @return possible patient condition type
     */
    Optional<ConditionType> findByDataTypeAndInternalCode(String dataType, String code);

    /**
     * @param dataType Type of data (patient infection or problem list)
     * @return list of all patient condition types
     */
    List<ConditionType> findAllByDataType(String dataType);
}
