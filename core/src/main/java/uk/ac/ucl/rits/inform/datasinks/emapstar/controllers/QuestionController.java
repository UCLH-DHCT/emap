package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionRepository;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestion;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestionAudit;

import java.time.Instant;
import java.util.Map;

/**
 * Interacts with repositories for questions and answers.
 * @author Stef Piatek
 */
@Component
public class QuestionController {
    private final QuestionRepository questionRepo;
    private final LabSampleQuestionRepository labSampleQuestionRepo;
    private final LabSampleQuestionAuditRepository labSampleQuestionAuditRepo;

    public QuestionController(
            QuestionRepository questionRepo,
            LabSampleQuestionRepository labSampleQuestionRepo, LabSampleQuestionAuditRepository labSampleQuestionAuditRepo) {
        this.questionRepo = questionRepo;
        this.labSampleQuestionRepo = labSampleQuestionRepo;
        this.labSampleQuestionAuditRepo = labSampleQuestionAuditRepo;
    }

    /**
     * Update or create lab order questions.
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

    private Question getOrCreateQuestion(String question) {
        return questionRepo.findByQuestion(question)
                .orElseGet(() -> questionRepo.save(new Question(question)));
    }
}
