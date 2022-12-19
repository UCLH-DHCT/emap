package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.forms;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswerAudit;

import java.util.List;

public interface FormAnswerAuditRepository extends CrudRepository<FormAnswerAudit, Long> {
    /**
     * Used in testing only.
     * @param id id of the form answer
     * @return true if the form answer exsits.
     */
    boolean existsByFormAnswerId(Long id);

    /**
     * Used in testing only.
     * @param internalId internal ID to query by
     * @return list of form answer audits
     */
    List<FormAnswerAudit> findAllByInternalId(String internalId);
}
