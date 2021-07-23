package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.RequestAnswerAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.RequestAnswerRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;
import uk.ac.ucl.rits.inform.informdb.questions.QuestionAudit;
import uk.ac.ucl.rits.inform.informdb.questions.RequestAnswer;
import uk.ac.ucl.rits.inform.informdb.questions.RequestAnswerAudit;


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
    private final RequestAnswerRepository requestAnswerRepo;
    private final RequestAnswerAuditRepository requestAnswerAuditRepo;

    /**
     * Initialising the repositories needed to store question information.
     * @param questionRepo              Repository with questions
     * @param questionAuditRepo         Repository for question auditing
     * @param requestAnswerRepo         Repository for answers to questions
     * @param requestAnswerAuditRepo    Repository for auditing answers to questions
     */
    public QuestionController(QuestionRepository questionRepo, QuestionAuditRepository questionAuditRepo,
                              RequestAnswerRepository requestAnswerRepo,
                              RequestAnswerAuditRepository requestAnswerAuditRepo) {
        this.questionRepo = questionRepo;
        this.questionAuditRepo = questionAuditRepo;
        this.requestAnswerRepo = requestAnswerRepo;
        this.requestAnswerAuditRepo = requestAnswerAuditRepo;
    }

    /**
     * Processing a set of questions and related answers (independent of data type they relate to).
     * @param questionsAndAnswers Map in form {question, answer}
     * @param parentId            Identifier for parent entity that triggered the creation of a question, e.g. lab
     *                            sample or consultation request
     * @param validFrom           most recent change to results
     * @param storedFrom          time that star started processing the message
     */
    void processQuestions(Map<String, String> questionsAndAnswers, long parentId, Instant validFrom,
                          Instant storedFrom) {
        for (Map.Entry<String, String> questionAndAnswer : questionsAndAnswers.entrySet()) {
            Question question = getOrCreateQuestion(questionAndAnswer.getKey(), validFrom, storedFrom);
            RequestAnswer answer = getOrCreateRequestAnswer(question, questionAndAnswer.getValue(), parentId,
                    validFrom, storedFrom);
        }
    }

    /**
     * Check whether question (based on the entire String) already exists in the respective table. If yes, return the
     * existing entity; if not, create a new entity based the relevant data.
     * @param question          Question as such.
     * @param validFrom         Time when question got changed most recently.
     * @param storedFrom        Time when star started question processing.
     * @return a specific question as stored in the question repository
     */
    @Cacheable(value = "question")
    public Question getOrCreateQuestion(String question, Instant validFrom, Instant storedFrom) {
        return questionRepo.findByQuestion(question)
                .orElseGet(() -> questionRepo.save(new Question(question, validFrom, storedFrom)));
    }

    /**
     * Check whether answer for entity triggering question already exists in the question repository or whether it would
     * need to change.
     * @param question          Question as such.
     * @param answer            Answer to the questions.
     * @param parentId          Parent entity that triggered the creation of question and answer for it.
     * @param validFrom         Time when question got changed most recently.
     * @param storedFrom        Time when star started question processing.
     * @return an answer to a question linked to a specific entity.
     */
    @Cacheable(value = "question")
    public RequestAnswer getOrCreateRequestAnswer(Question question, String answer, long parentId, Instant validFrom,
                                                  Instant storedFrom) {
        return requestAnswerRepo.findByQuestionIdAndParentIdAndAnswer(question, parentId, answer)
                .orElseGet(() -> requestAnswerRepo.save(new RequestAnswer(question, answer, parentId, validFrom,
                        storedFrom)));
    }

    /**
     * Update or create questions.
     * @param question          Question in relation to lab sample.
     * @param answer            Answer to the question raised through data type.
     * @param parentId          Entity that triggered the creation of question-answer pair.
     * @param validFrom         Time when question got changed most recently.
     * @param storedFrom        Time when star started question processing.
     */
    private void updateOrCreateQuestion(Question question, String answer, long parentId, Instant validFrom,
                                        Instant storedFrom) {
        RowState<Question, QuestionAudit> questionState = questionRepo
                .findByQuestion(question)
                .map(q -> new RowState<>(q, validFrom, storedFrom, false))
                .orElseGet(() -> {
                            Question q = new Question(question.getQuestion(), validFrom, storedFrom);
                            return new RowState<>(q, validFrom, storedFrom, true);
                        }
                );
        Question questionEntity = questionState.getEntity();

        RowState<RequestAnswer, RequestAnswerAudit> answerState = requestAnswerRepo
                .findByQuestionIdAndParentIdAndAnswer(question, parentId, answer)
                .map(a -> new RowState<>(a, validFrom, storedFrom, false))
                .orElseGet(() -> {
                            RequestAnswer a = new RequestAnswer(questionEntity, answer, parentId, validFrom, storedFrom);
                            return new RowState<>(a, validFrom, storedFrom, true);
                        }
                );
        RequestAnswer answerEntity = answerState.getEntity();

        questionState.saveEntityOrAuditLogIfRequired(questionRepo, questionAuditRepo);
        answerState.saveEntityOrAuditLogIfRequired(requestAnswerRepo, requestAnswerAuditRepo);
    }

}
