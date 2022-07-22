package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovementAudit;

/**
 * Interaction with the PlannedMovementAudit table.
 * @author Stef Piatek
 */
public interface PlannedMovementAuditRepository extends CrudRepository<PlannedMovementAudit, Long> {
}
