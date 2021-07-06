package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestionAudit;

/**
 * Consultation request question audit repository.
 * @author Anika Cawthorn
 */
public interface ConsultationRequestQuestionAuditRepository extends CrudRepository<ConsultationRequestQuestionAudit, Long> {
}
