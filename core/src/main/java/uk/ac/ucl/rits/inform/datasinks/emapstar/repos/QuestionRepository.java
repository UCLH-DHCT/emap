package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.Question;

import java.util.Optional;

/**
 * Lab Order Question repository.
 * @author Stef Piatek
 */
public interface QuestionRepository extends CrudRepository<Question, Long> {
    Optional<Question> findByQuestion(String question);
}
