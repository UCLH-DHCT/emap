package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion;

import java.util.Optional;

/**
 * Consultation request question repository.
 * @author Anika Cawthorn
 */
public interface ConsultationRequestQuestionRepository extends CrudRepository<ConsultationRequestQuestion, Long> {
    Optional<ConsultationRequestQuestion> findByConsultationRequestIdAndQuestionId(ConsultationRequest consultationRequest, Question question);

    /**
     * For testing.
     * @param question question string
     * @return possible lab order question
     */
    Optional<ConsultationRequestQuestion> findByQuestionIdQuestion(String question);
}
