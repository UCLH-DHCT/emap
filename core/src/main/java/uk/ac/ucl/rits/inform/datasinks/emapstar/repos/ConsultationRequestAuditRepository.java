package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;


import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestAudit;

/**
 * Consultation request audit repository.
 * @author Anika Cawthorn
 */
public interface ConsultationRequestAuditRepository extends CrudRepository<ConsultationRequestAudit, Long> {
}

