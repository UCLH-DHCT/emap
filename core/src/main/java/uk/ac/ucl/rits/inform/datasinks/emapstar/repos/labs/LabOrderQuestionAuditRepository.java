package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderQuestionAudit;

/**
 * Lab Order Question Audit repository.
 * @author Stef Piatek
 */
public interface LabOrderQuestionAuditRepository extends CrudRepository<LabOrderQuestionAudit, Long> {
}
