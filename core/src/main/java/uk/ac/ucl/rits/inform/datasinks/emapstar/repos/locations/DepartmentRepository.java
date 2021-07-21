package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Department;

import java.util.Optional;

/**
 * Department repository.
 * @author Stef Piatek
 */
public interface DepartmentRepository extends CrudRepository<Department, Long> {
    Optional<Department> findByHl7String(String hl7String);
}
