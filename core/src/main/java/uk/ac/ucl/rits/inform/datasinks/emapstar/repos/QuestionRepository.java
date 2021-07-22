package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;

import java.util.Optional;

/**
 * Question repository to record all questions independent to which data type they belong to. Each question is linked
 * to a parent table identifier and parent table id.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public interface QuestionRepository extends CrudRepository<Question, Long> {
    /**
     * Find specific question based on its string content and the identifier in the parent table.
     * @param question
     * @param parentTableId
     * @return
     */
    Optional<Question> findByQuestionAndParentTableIdentifier(Question question, long parentTableId);

    /**
     * Find question based on its string content and the identifier in the parent table this question is related to,
     * e.g. a lab sample identifier. This method is for testing purposes only.
     * @param question
     * @param parentTableId
     * @return
     */
    Optional<Question> findByQuestionAndParentTableIdentifier(String question, String parentTableId);
}
