package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptomAudit;

/**
 * PatientConditionAudit repository.
 * @author Tom Young
 */
public interface ConditionSymptomAuditRepository extends CrudRepository<ConditionSymptomAudit, Long> {

}
