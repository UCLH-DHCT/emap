package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationTypeAudit;

/**
 * Interacts with the ConsultationTypeAudit table.
 * @author Stef Piatek
 */
public interface ConsultationTypeAuditRepository extends CrudRepository<ConsultationTypeAudit, Long> {
}



