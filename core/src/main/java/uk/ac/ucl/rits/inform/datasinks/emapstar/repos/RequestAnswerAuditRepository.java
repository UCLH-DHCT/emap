package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.questions.RequestAnswerAudit;

public interface RequestAnswerAuditRepository extends CrudRepository<RequestAnswerAudit, Long> {
}
