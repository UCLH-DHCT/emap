package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderQuestion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Lab Order Question repository.
 * @author Stef Piatek
 */
public interface LabOrderQuestionRepository extends CrudRepository<LabOrderQuestion, Long> {
    Optional<LabOrderQuestion> findByLabOrderIdAndQuestionId(LabOrder labOrder, Question question);

    List<LabOrderQuestion> findAllByLabOrderIdAndValidFromIsBefore(LabOrder labOrder, Instant deleteTime);

    /**
     * For testing.
     * @param question question string
     * @return possible lab order question
     */
    Optional<LabOrderQuestion> findByQuestionIdQuestion(String question);
}
