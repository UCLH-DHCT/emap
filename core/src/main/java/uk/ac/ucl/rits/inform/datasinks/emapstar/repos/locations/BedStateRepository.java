package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.BedState;

/**
 * Bed repository.
 * @author Stef Piatek
 */
public interface BedStateRepository extends CrudRepository<BedState, Long> {
}
