package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;

import java.util.List;

/**
 * Lab result audit repository.
 * @author Stef Piatek
 */
public interface LabResultAuditRepository extends CrudRepository<LabResultAudit, Long> {

    List<LabResultAudit> findAllByLabOrderIdIn(Iterable<Long> labOrderIds);
}
