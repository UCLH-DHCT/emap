package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestionAudit;

/**
 * Lab Order Question Audit repository.
 * @author Stef Piatek
 */
public interface LabSampleQuestionAuditRepository extends CrudRepository<LabSampleQuestionAudit, Long> {
}
