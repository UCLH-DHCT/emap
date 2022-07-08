package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswerAudit;

public interface FormAnswerAuditRepository extends CrudRepository<FormAnswerAudit, Long> {
    boolean existsByFormAnswerId(Long id);
}
