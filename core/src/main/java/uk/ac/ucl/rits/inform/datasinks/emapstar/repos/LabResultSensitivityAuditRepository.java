package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivityAudit;

/**
 * Lab result sensitivity audit repository.
 * @author Stef Piatek
 */
public interface LabResultSensitivityAuditRepository extends CrudRepository<LabResultSensitivityAudit, Long> {

}
