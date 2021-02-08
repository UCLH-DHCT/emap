package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollectionAudit;

/**
 * Lab Collection Audit repository.
 * @author Stef Piatek
 */
public interface LabCollectionAuditRepository extends CrudRepository<LabCollectionAudit, Long> {
}
