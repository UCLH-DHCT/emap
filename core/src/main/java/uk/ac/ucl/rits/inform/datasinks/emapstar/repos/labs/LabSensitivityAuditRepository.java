package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivityAudit;

/**
 * Lab sensitivity audit repository.
 * @author Stef Piatek
 */
public interface LabSensitivityAuditRepository extends CrudRepository<LabSensitivityAudit, Long> {

}
