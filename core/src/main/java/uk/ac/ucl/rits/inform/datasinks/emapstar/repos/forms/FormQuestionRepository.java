package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.forms;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestion;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FormQuestionRepository extends CrudRepository<FormQuestion, Long> {
    Optional<FormQuestion> findByInternalId(String internalId);

    /**
     * Only used in testing.
     * @param internalIds internal IDs to query by
     * @return list of form questions
     */
    List<FormQuestion> findAllByInternalIdIn(Set<String> internalIds);
}
