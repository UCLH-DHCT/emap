package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;

/**
 * Lab order repository.
 * @author Stef Piatek
 */
public interface LabOrderRepository extends CrudRepository<LabOrder, Long> {
}
