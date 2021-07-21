package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.RoomState;

import java.util.Optional;

/**
 * RoomState repository.
 * @author Stef Piatek
 */
public interface RoomStateRepository extends CrudRepository<RoomState, Long> {
    Optional<RoomState> findByCsn(long Csn);
}
