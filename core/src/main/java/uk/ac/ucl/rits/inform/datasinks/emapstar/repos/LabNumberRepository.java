package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;

/**
 * Lab number repository.
 * @author Stef Piatek
 */
public interface LabNumberRepository extends CrudRepository<LabNumber, Long> {
}
