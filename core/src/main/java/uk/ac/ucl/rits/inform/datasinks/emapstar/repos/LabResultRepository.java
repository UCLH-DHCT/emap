package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;

/**
 * Lab result repository.
 * @author Stef Piatek
 */
public interface LabResultRepository extends CrudRepository<LabResult, Long> {
}
