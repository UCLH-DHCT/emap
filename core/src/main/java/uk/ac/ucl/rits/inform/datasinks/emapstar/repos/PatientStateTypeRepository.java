package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateType;

/**
 * PatientStateType repository.
 * @author Stef Piatek
 */
public interface PatientStateTypeRepository extends CrudRepository<PatientStateType, Long> {
}
