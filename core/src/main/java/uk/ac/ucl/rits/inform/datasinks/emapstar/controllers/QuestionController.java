package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;
import uk.ac.ucl.rits.inform.informdb.QuestionAudit;

import java.time.Instant;
import java.util.Map;

/**
 * Creates or updates information in relation to questions, which can be linked to several data types (parent table
 * information).
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
@Component
@EnableCaching
public class QuestionController {
    private final QuestionRepository questionRepo;
    private final QuestionAuditRepository questionAuditRepo;

    public enum ParentTableType {
        CONSULTATION_REQUEST,
        LAB_SAMPLE
    }

    public QuestionController(QuestionRepository questionRepo, QuestionAuditRepository questionAuditRepo) {
        this.questionRepo = questionRepo;
        this.questionAuditRepo = questionAuditRepo;
    }

    /**
     * Processing a set of questions and related answers (independent of data type they relate to).
     * @param questionsAndAnswers Map in form {question, answer}
     * @param validFrom           most recent change to results
     * @param storedFrom          time that star started processing the message
     */
    void processQuestions(Map<String, String> questionsAndAnswers, String parentTableType, long parentTableId,
                          Instant validFrom, Instant storedFrom) {
        for (Map.Entry<String, String> questionAndAnswer : questionsAndAnswers.entrySet()) {
            Question question = getOrCreateQuestion(questionAndAnswer.getKey(), parentTableType, parentTableId,
                    validFrom, storedFrom);
            updateOrCreateQuestion(question, questionAndAnswer.getValue(), parentTableType, parentTableId,
                    validFrom, storedFrom);
        }
    }

    /**
     * Update or create questions.
     * @param question          Question in relation to lab sample.
     * @param answer            Answer in relation to lab sample question.
     * @param parentTableType   As question might be related to different types of data, this is an indicator for the
     *                          respective data type the question belongs to.
     * @param parentTableTypeId Identifier of entity in parent table the question belongs to.
     * @param validFrom         Time when question got changed most recently.
     * @param storedFrom        Time when star started question processing.
     */
    private void updateOrCreateQuestion(Question question, String answer, String parentTableType,
                                        long parentTableTypeId, Instant validFrom, Instant storedFrom) {
        RowState<Question, QuestionAudit> questionState = questionRepo
                .findByQuestion(question)
                .map(q -> new RowState<>(q, validFrom, storedFrom, false))
                .orElseGet(() -> {
                            Question q = new Question(question.getQuestion(), parentTableType, parentTableTypeId,
                                    validFrom, storedFrom);
                            return new RowState<>(q, validFrom, storedFrom, true);
                        }
                );
        Question questionEntity = questionState.getEntity();

        if (questionState.isEntityCreated() || validFrom.isAfter(questionEntity.getValidFrom())) {
            questionState.assignIfDifferent(answer, questionEntity.getAnswer(), questionEntity::setAnswer);
        }
        questionState.saveEntityOrAuditLogIfRequired(questionRepo, questionAuditRepo);
    }

    /**
     * Check whether question (based on the entire String) already exists in the respective table. If yes, return the
     * existing entity; if not, create a new entity based the relevant data.
     * @param question          Question as such.
     * @param parentTableType   Data type question belongs to, e.g. lab sample or consultation request.
     * @param parentTableId     Identifier in the parent table this question belongs to.
     * @param validFrom         Time when question got changed most recently.
     * @param storedFrom        Time when star started question processing.
     * @return
     */
    @Cacheable(value = "question")
    public Question getOrCreateQuestion(String question, String parentTableType, long parentTableId, Instant validFrom,
                                        Instant storedFrom) {
        return questionRepo.findByQuestion(question)
                .orElseGet(() -> questionRepo.save(new Question(question, parentTableType, parentTableId,
                        validFrom, storedFrom)));
    }
}
