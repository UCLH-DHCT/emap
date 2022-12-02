package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestion;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FormQuestionRepository extends CrudRepository<FormQuestion, Long> {
    Optional<FormQuestion> findByInternalId(String formQuestionSourceId);

    // only used in testing
    List<FormQuestion> findAllByInternalIdIn(Set<String> formQuestionSourceIds);
}
