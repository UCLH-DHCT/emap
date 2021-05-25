package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateType;

import java.util.Optional;

/**
 * PatientStateType repository.
 * @author Stef Piatek
 */
public interface PatientStateTypeRepository extends CrudRepository<PatientStateType, Long> {
    /**
     * @param dataType Type of data (patient infection or problem list)
     * @param name     name of state
     * @return possible patient state type
     */
    Optional<PatientStateType> findByDataTypeAndName(String dataType, String name);
}
