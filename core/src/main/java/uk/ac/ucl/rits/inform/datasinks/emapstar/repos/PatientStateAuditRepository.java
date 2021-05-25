package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateAudit;

/**
 * PatientStateAudit repository.
 * @author Stef Piatek
 */
public interface PatientStateAuditRepository extends CrudRepository<PatientStateAudit, Long> {
}
