package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;

public interface FormAnswerRepository extends CrudRepository<FormAnswer, Long> {
    Iterable<FormAnswer> findAllByInternalIdIn(Iterable<String> internalId);
}
