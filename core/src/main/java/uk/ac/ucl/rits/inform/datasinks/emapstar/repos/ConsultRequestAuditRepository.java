package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;


import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultRequestAudit;

/**
 * Consultation request audit repository.
 * @author Anika Cawthorn
 */
public interface ConsultRequestAuditRepository extends CrudRepository<ConsultRequestAudit, Long> {
}

