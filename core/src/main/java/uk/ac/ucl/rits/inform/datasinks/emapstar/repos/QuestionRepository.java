package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;

import java.util.Optional;

/**
 * Question repository to record all questions independent of which data type they belong to.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public interface QuestionRepository extends CrudRepository<Question, Long> {
    /**
     * Find question based on its string content and the identifier in the parent table this question is related to,
     * e.g. a lab sample identifier. This method is for testing purposes only.
     * @param question  Question string to search for in the repository.
     * @return question as defined by question string
     */
    Optional<Question> findByQuestion(String question);
}
