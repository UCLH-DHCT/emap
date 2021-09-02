package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecisionTypeAudit;

/**
 * Holds information on changes of types of advanced decisions.
 * @author Anika Cawthorn
 */
public interface AdvancedDecisionTypeAuditRepository extends CrudRepository<AdvancedDecisionTypeAudit, Long> {
}
