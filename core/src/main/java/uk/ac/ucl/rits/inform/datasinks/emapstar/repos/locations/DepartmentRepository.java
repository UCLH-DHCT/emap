package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Department;

/**
 * Department repository.
 * @author Stef Piatek
 */
public interface DepartmentRepository extends CrudRepository<Department, Long> {
}
