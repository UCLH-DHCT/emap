package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;
import uk.ac.ucl.rits.inform.informdb.questions.RequestAnswer;

import java.util.Optional;

/**
 * Repository for answers to questions and their links to the parent entity.
 * @author Anika Cawthorn
 */
public interface RequestAnswerRepository extends CrudRepository<RequestAnswer, Long> {
    /**
     * Get answer to question for a specific entity.
     * @param question  Question for which answer is relevant.
     * @param parentId  Entity that triggered the creation of this question-answer pair.
     * @param answer    Content of the answer to the question as triggered by entity question.
     * @return possible patient condition
     */
    Optional<RequestAnswer> findByQuestionIdAndParentIdAndAnswer(Question question, long parentId, String answer);
}
