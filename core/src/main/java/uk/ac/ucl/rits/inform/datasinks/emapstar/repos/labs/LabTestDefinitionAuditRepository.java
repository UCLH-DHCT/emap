package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinitionAudit;

/**
 * Interacting with the lab test definition audit table.
 * @author Stef Piatek
 */
public interface LabTestDefinitionAuditRepository extends CrudRepository<LabTestDefinitionAudit, Long> {
}
