package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecisionAudit;

/**
 * Holds historic changes to advanced decisions registered for patients.
 * @author Anika Cawthorn
 */
public interface AdvanceDecisionAuditRepository extends CrudRepository<AdvanceDecisionAudit, Long> {
}
