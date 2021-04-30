package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.state.PatientState;

/**
 * PatientState repository.
 * @author Stef Piatek
 */
public interface PatientStateRepository extends CrudRepository<PatientState, Long> {
}
