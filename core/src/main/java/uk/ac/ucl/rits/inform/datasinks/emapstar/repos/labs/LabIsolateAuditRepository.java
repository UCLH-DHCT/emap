package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolateAudit;

/**
 * Lab Isolate audit repository.
 * @author Stef Piatek
 */
public interface LabIsolateAuditRepository extends CrudRepository<LabIsolateAudit, Long> {
}
