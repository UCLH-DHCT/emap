package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionRepository;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestion;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestionAudit;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestionAudit;

import java.time.Instant;
import java.util.Map;

/**
 * Interacts with repositories for questions and answers.
 * @author Stef Piatek
 */
@Component
@EnableCaching
public class QuestionController {
    private final QuestionRepository questionRepo;
    private final LabSampleQuestionRepository labSampleQuestionRepo;
    private final LabSampleQuestionAuditRepository labSampleQuestionAuditRepo;
    private final ConsultationRequestQuestionRepository consultationRequestQuestionRepo;
    private final ConsultationRequestQuestionAuditRepository consultationRequestQuestionAuditRepo;

    public QuestionController(
            QuestionRepository questionRepo,
            LabSampleQuestionRepository labSampleQuestionRepo, LabSampleQuestionAuditRepository labSampleQuestionAuditRepo,
            ConsultationRequestQuestionRepository consultationRequestQuestionRepo,
            ConsultationRequestQuestionAuditRepository consultationRequestQuestionAuditRepo) {
        this.questionRepo = questionRepo;
        this.labSampleQuestionRepo = labSampleQuestionRepo;
        this.labSampleQuestionAuditRepo = labSampleQuestionAuditRepo;
        this.consultationRequestQuestionRepo = consultationRequestQuestionRepo;
        this.consultationRequestQuestionAuditRepo = consultationRequestQuestionAuditRepo;
    }

    /**
     * Processing lab order questions.
     * @param questionsAndAnswers Map in form {question, answer}
     * @param labSample           Lab sample entity
     * @param validFrom           most recent change to results
     * @param storedFrom          time that star started processing the message
     */
    void processLabOrderQuestions(Map<String, String> questionsAndAnswers, LabSample labSample, Instant validFrom, Instant storedFrom) {
        for (Map.Entry<String, String> questionAndAnswer : questionsAndAnswers.entrySet()) {
            Question question = getOrCreateQuestion(questionAndAnswer.getKey());
            updateOrCreateLabOrderQuestion(labSample, question, questionAndAnswer.getValue(), validFrom, storedFrom);
        }
    }

    /**
     * Update or create lab order questions.
     * @param labSample     Lab sample question relates to.
     * @param question      Question in relation to lab sample.
     * @param answer        Answer in relation to lab sample question.
     * @param validFrom     Time when lab sample question got changed most recently.
     * @param storedFrom    Time when star started lab sample question processing.
     */
    private void updateOrCreateLabOrderQuestion(
            LabSample labSample, Question question, String answer, Instant validFrom, Instant storedFrom) {
        RowState<LabSampleQuestion, LabSampleQuestionAudit> questionState = labSampleQuestionRepo
                .findByLabSampleIdAndQuestionId(labSample, question)
                .map(q -> new RowState<>(q, validFrom, storedFrom, false))
                .orElseGet(() -> {
                            LabSampleQuestion q = new LabSampleQuestion(labSample, question, validFrom, storedFrom);
                            return new RowState<>(q, validFrom, storedFrom, true);
                        }
                );
        LabSampleQuestion labSampleQuestion = questionState.getEntity();

        if (questionState.isEntityCreated() || validFrom.isAfter(labSampleQuestion.getValidFrom())) {
            questionState.assignIfDifferent(answer, labSampleQuestion.getAnswer(), labSampleQuestion::setAnswer);
        }
        questionState.saveEntityOrAuditLogIfRequired(labSampleQuestionRepo, labSampleQuestionAuditRepo);
    }

    /**
     * Recording or updating questions and potentially answers to these questions, posed in relation to a consultation
     * request.
     * @param questionsAndAnswers Map in form {question, answer}
     * @param consultationRequest Consultation request entity
     * @param validFrom           most recent change to results
     * @param storedFrom          time that star started processing the message
     */
    void processConsultationRequestQuestions(Map<String, String> questionsAndAnswers, ConsultationRequest consultationRequest,
                                  Instant validFrom, Instant storedFrom) {
        for (Map.Entry<String, String> questionAndAnswer : questionsAndAnswers.entrySet()) {
            Question question = getOrCreateQuestion(questionAndAnswer.getKey());
            updateOrCreateConsultationRequestQuestion(consultationRequest, question, questionAndAnswer.getValue(),
                    validFrom, storedFrom);
        }
    }

    /**
     * Creating or updating consultation request question.
     * @param consultationRequest Consultation request question relates to.
     * @param question            Question posed in relation to consultation request.
     * @param answer              Answer to question.
     * @param validFrom           Most recent change to question.
     * @param storedFrom          Time when star started processing this question.
     */
    private void updateOrCreateConsultationRequestQuestion(ConsultationRequest consultationRequest, Question question,
                                                String answer, Instant validFrom, Instant storedFrom) {
        RowState<ConsultationRequestQuestion, ConsultationRequestQuestionAudit> questionState = consultationRequestQuestionRepo
                .findByConsultationRequestIdAndQuestionId(consultationRequest, question)
                .map(q -> new RowState<>(q, validFrom, storedFrom, false))
                .orElseGet(() -> {
                            ConsultationRequestQuestion q = new ConsultationRequestQuestion(consultationRequest,
                                    question, validFrom, storedFrom);
                            return new RowState<>(q, validFrom, storedFrom, true);
                        }
                );
        ConsultationRequestQuestion consultationRequestQuestion = questionState.getEntity();

        if (questionState.isEntityCreated() || validFrom.isAfter(consultationRequestQuestion.getValidFrom())) {
            questionState.assignIfDifferent(answer, consultationRequestQuestion.getAnswer(), consultationRequestQuestion::setAnswer);
        }
        questionState.saveEntityOrAuditLogIfRequired(consultationRequestQuestionRepo, consultationRequestQuestionAuditRepo);
    }

    @Cacheable(value = "question")
    public Question getOrCreateQuestion(String question) {
        return questionRepo.findByQuestion(question)
                .orElseGet(() -> questionRepo.save(new Question(question)));
    }
}
