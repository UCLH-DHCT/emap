package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.informdb.movement.RoomState;

import java.util.List;
import java.util.Optional;

/**
 * RoomState repository.
 * @author Stef Piatek
 */
public interface RoomStateRepository extends CrudRepository<RoomState, Long> {
    List<RoomState> findAllByRoomIdOrderByValidFromDesc(Room roomId);

    /**
     * For testing.
     * @param csn csn to find
     * @return potential room state
     */
    Optional<RoomState> findByCsn(long csn);
}
