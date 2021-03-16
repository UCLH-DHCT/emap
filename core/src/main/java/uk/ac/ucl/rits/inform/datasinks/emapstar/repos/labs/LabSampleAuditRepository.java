package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleAudit;

/**
 * Lab Sample Audit repository.
 * @author Stef Piatek
 */
public interface LabSampleAuditRepository extends CrudRepository<LabSampleAudit, Long> {
}
