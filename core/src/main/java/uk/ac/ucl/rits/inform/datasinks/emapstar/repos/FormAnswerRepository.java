package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;

import java.util.List;

public interface FormAnswerRepository extends CrudRepository<FormAnswer, Long> {
    List<FormAnswer> findAllByInternalIdIn(Iterable<String> internalId);
}
