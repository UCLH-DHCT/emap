package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;

/**
 * Lab Order Audit repository.
 * @author Stef Piatek
 */
public interface LabOrderAuditRepository extends CrudRepository<LabOrderAudit, Long> {
}
