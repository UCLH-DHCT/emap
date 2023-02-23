package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovementAudit;

import java.util.List;

/**
 * Interaction with the PlannedMovementAudit table.
 * @author Stef Piatek
 */
public interface PlannedMovementAuditRepository extends CrudRepository<PlannedMovementAudit, Long> {
    /**
     * For testing.
     * @param hospitalVisitId id of the hospital visit
     * @return all planned movement audits
     */
    List<PlannedMovementAudit> findAllByHospitalVisitId(long hospitalVisitId);
}
