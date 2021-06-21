package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;

/**
 * PatientConditionAudit repository.
 * @author Stef Piatek
 */
public interface PatientConditionAuditRepository extends CrudRepository<PatientConditionAudit, Long> {
}
