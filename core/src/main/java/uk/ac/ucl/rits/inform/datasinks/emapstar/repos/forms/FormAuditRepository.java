package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.forms;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormAudit;

public interface FormAuditRepository extends CrudRepository<FormAudit, Long> {
    /**
     * For testing only.
     * @param formId the id to query by
     * @return true if the form exists
     */
    boolean existsByFormId(Long formId);
}
