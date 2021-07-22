package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestion;

import java.util.Optional;

/**
 * Lab Sample Question repository.
 * @author Stef Piatek
 */
public interface LabSampleQuestionRepository extends CrudRepository<LabSampleQuestion, Long> {
    Optional<LabSampleQuestion> findByLabSampleIdAndQuestionId(LabSample labSample, Question question);

    /**
     * For testing.
     * @param question question string
     * @return possible lab order question
     */
    Optional<LabSampleQuestion> findByQuestionIdQuestion(String question);
}
