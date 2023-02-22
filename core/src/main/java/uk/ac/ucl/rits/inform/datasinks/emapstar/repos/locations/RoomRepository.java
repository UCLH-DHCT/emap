package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Department;
import uk.ac.ucl.rits.inform.informdb.movement.Room;

import java.util.Optional;

/**
 * Room repository.
 * @author Stef Piatek
 */
public interface RoomRepository extends CrudRepository<Room, Long> {
    Optional<Room> findByHl7StringAndDepartmentId(String hl7String, Department departmentId);
}
