package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.QuestionAudit;

/**
 * Question audit repository.
 * @author Anika Cawthorn
 */
public interface QuestionAuditRepository extends CrudRepository<QuestionAudit, Long> {
}


