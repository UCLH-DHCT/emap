package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.RequestAnswerAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.RequestAnswerRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;
import uk.ac.ucl.rits.inform.informdb.questions.RequestAnswer;
import uk.ac.ucl.rits.inform.informdb.questions.RequestAnswerAudit;

import java.time.Instant;
import java.util.Map;

enum ParentTableType {
    CONSULT_REQUEST,
    LAB_SAMPLE
}

/**
 * Creates or updates information in relation to questions, which can be linked to several data types (parent table
 * information).
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
@Component
@EnableCaching
public class QuestionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final QuestionRepository questionRepo;
    private final RequestAnswerRepository requestAnswerRepo;
    private final RequestAnswerAuditRepository requestAnswerAuditRepo;

    /**
     * Initialising the repositories needed to store question information.
     * @param questionRepo           Repository with questions
     * @param requestAnswerRepo      Repository for answers to questions
     * @param requestAnswerAuditRepo Repository for auditing answers to questions
     */
    public QuestionController(QuestionRepository questionRepo, RequestAnswerRepository requestAnswerRepo,
                              RequestAnswerAuditRepository requestAnswerAuditRepo) {
        this.questionRepo = questionRepo;
        this.requestAnswerRepo = requestAnswerRepo;
        this.requestAnswerAuditRepo = requestAnswerAuditRepo;
    }

    /**
     * Processing a set of questions and related answers (independent of data type they relate to).
     * @param questionsAndAnswers Map in form {question, answer}
     * @param parentTable         Data type that triggered the creation of question-answer pair.
     * @param parentId            Identifier for parent entity that triggered the creation of a question, e.g. lab
     *                            sample or consultation request
     * @param validFrom           most recent change to results
     * @param storedFrom          time that star started processing the message
     */
    void processQuestions(Map<String, String> questionsAndAnswers, String parentTable, long parentId, Instant validFrom,
                          Instant storedFrom) {
        for (Map.Entry<String, String> questionAndAnswer : questionsAndAnswers.entrySet()) {
            Question question = getOrCreateQuestion(questionAndAnswer.getKey(), validFrom, storedFrom);

            RowState<RequestAnswer, RequestAnswerAudit> answerState = getOrCreateRequestAnswer(question,
                    questionAndAnswer.getValue(), parentTable, parentId, validFrom, storedFrom);

            if (requestAnswerShouldBeUpdated(validFrom, answerState)) {
                updateRequestAnswer(questionAndAnswer.getValue(), answerState);
            }
            answerState.saveEntityOrAuditLogIfRequired(requestAnswerRepo, requestAnswerAuditRepo);
        }
    }

    /**
     * Check whether question (based on the entire String) already exists in the respective table. If yes, return the
     * existing entity; if not, create a new entity based the relevant data.
     * @param question   Question as such.
     * @param validFrom  Time when question got changed most recently.
     * @param storedFrom Time when star started question processing.
     * @return a specific question as stored in the question repository
     */
    @Cacheable(value = "question", key = "{#question}")
    public Question getOrCreateQuestion(String question, Instant validFrom, Instant storedFrom) {
        return questionRepo
                .findByQuestion(question)
                .orElseGet(() -> createQuestion(question, validFrom, storedFrom));
    }

    /**
     * Creates new question from the information provided and wraps it with RowState.
     * @param questionString Content of the question as opposed to table row in Star.
     * @param validFrom      When this question is valid from.
     * @param storedFrom     When EMAP has started processing this entity.
     * @return a generated Question
     */
    public Question createQuestion(String questionString, Instant validFrom,
                                   Instant storedFrom) {
        Question question = new Question(questionString, validFrom, storedFrom);
        logger.debug("Created new {}", question);
        questionRepo.save(question);
        return question;
    }

    /**
     * Check whether answer for entity triggering question already exists in the question repository or whether it would
     * need to change.
     * @param question    Question as such.
     * @param answer      Answer to the questions.
     * @param parentTable Data type that triggered the creation of question-answer pair.
     * @param parentId    Parent entity that triggered the creation of question and answer for it.
     * @param validFrom   Time when question got changed most recently.
     * @param storedFrom  Time when star started question processing.
     * @return an answer to a question linked to a specific entity.
     */
    public RowState<RequestAnswer, RequestAnswerAudit> getOrCreateRequestAnswer(Question question,
                                                                                String answer, String parentTable,
                                                                                long parentId, Instant validFrom,
                                                                                Instant storedFrom) {
        return requestAnswerRepo
                .findByQuestionIdAndParentId(question, parentId)
                .map(r -> new RowState<>(r, validFrom, storedFrom, false))
                .orElseGet(() -> createRequestAnswer(question, answer, parentTable, parentId, validFrom, storedFrom));
    }

    /**
     * Create answer for question.
     * @param question    Question answer belongs to.
     * @param answer      Answer content.
     * @param parentTable Data type that triggered the creation of question-answer pair.
     * @param parentId    Entity that triggered creation of question and answer.
     * @param validFrom   When information for entity is valid from.
     * @param storedFrom  When EMAP started processing this entity type.
     * @return a RequestAnswer wrapped in a RowState
     */
    public RowState<RequestAnswer, RequestAnswerAudit> createRequestAnswer(Question question, String answer,
                                                                           String parentTable, long parentId,
                                                                           Instant validFrom, Instant storedFrom) {
        RequestAnswer requestAnswer = new RequestAnswer(question, answer, parentTable, parentId, validFrom, storedFrom);
        logger.debug("Created new {}", requestAnswer);
        return new RowState<>(requestAnswer, validFrom, storedFrom, true);
    }

    /**
     * Decides whether or not the answer held for an existing question needs to be changed or not.
     * @param validFrom   Time of message that triggered update check
     * @param answerState Answer for question that is being processed
     * @return true if message should be updated
     */
    private boolean requestAnswerShouldBeUpdated(Instant validFrom, RowState<RequestAnswer,
            RequestAnswerAudit> answerState) {
        return (answerState.isEntityCreated() || !validFrom.isBefore(
                answerState.getEntity().getValidFrom()));
    }

    /**
     * Update answer for an existing question for a data type.
     * @param answer      Newly supplied answer
     * @param answerState Answer as previously provided for question.
     */
    private void updateRequestAnswer(String answer, RowState<RequestAnswer, RequestAnswerAudit> answerState) {
        answerState.assignIfDifferent(answer, answerState.getEntity().getAnswer(), answerState.getEntity()::setAnswer);
    }

}
