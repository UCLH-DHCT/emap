package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormAudit;

public interface FormAuditRepository extends CrudRepository<FormAudit, Long> {
    FormAudit findByFormId(Long formId);

    boolean existsByFormId(Long formId);
}
