package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionTypeAudit;

/**
 * ConditionTypeAudit repository.
 * @author Stef Piatek
 */
public interface ConditionTypeAuditRepository extends CrudRepository<ConditionTypeAudit, Long> {

}
