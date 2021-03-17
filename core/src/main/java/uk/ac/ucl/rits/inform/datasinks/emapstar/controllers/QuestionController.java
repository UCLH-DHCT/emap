package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderQuestionRepository;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderQuestion;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderQuestionAudit;

import java.time.Instant;

/**
 * Interacts with repositories for questions and answers.
 * @author Stef Piatek
 */
@Component
public class QuestionController {
    private final QuestionRepository questionRepo;
    private final LabOrderQuestionRepository labOrderQuestionRepo;
    private final LabOrderQuestionAuditRepository labOrderQuestionAuditRepo;

    public QuestionController(
            QuestionRepository questionRepo,
            LabOrderQuestionRepository labOrderQuestionRepo, LabOrderQuestionAuditRepository labOrderQuestionAuditRepo) {
        this.questionRepo = questionRepo;
        this.labOrderQuestionRepo = labOrderQuestionRepo;
        this.labOrderQuestionAuditRepo = labOrderQuestionAuditRepo;
    }

    /**
     * Delete all lab questions before deletion time.
     * @param labOrder     lab order
     * @param deletionTime deletion time
     * @param storedFrom   time that star started processing the message
     */
    void deleteAllLabQuestions(LabOrder labOrder, Instant deletionTime, Instant storedFrom) {
        labOrderQuestionRepo.findAllByLabOrderIdAndValidFromIsBefore(labOrder, deletionTime)
                .forEach(question -> {
                    LabOrderQuestionAudit audit = question.createAuditEntity(deletionTime, storedFrom);
                    labOrderQuestionAuditRepo.save(audit);
                    labOrderQuestionRepo.delete(question);
                });
    }

    /**
     * Update or create lab order question from pair.
     * @param questionAndAnswer Pair in form {question, answer}
     * @param labOrder          lab order entity
     * @param validFrom         most recent change to results
     * @param storedFrom        time that star started processing the message
     */
    void processLabOrderQuestion(Pair<String, String> questionAndAnswer, LabOrder labOrder, Instant validFrom, Instant storedFrom) {
        Question question = getOrCreateQuestion(questionAndAnswer.getLeft());
        updateOrCreateLabOrderQuestion(labOrder, question, questionAndAnswer.getRight(), validFrom, storedFrom);
    }

    private void updateOrCreateLabOrderQuestion(
            LabOrder labOrder, Question question, String answer, Instant validFrom, Instant storedFrom) {
        RowState<LabOrderQuestion, LabOrderQuestionAudit> questionState = labOrderQuestionRepo
                .findByLabOrderIdAndQuestionId(labOrder, question)
                .map(q -> new RowState<>(q, validFrom, storedFrom, false))
                .orElseGet(() -> {
                            LabOrderQuestion q = new LabOrderQuestion(labOrder, question, validFrom, storedFrom);
                            return new RowState<>(q, validFrom, storedFrom, true);
                        }
                );
        LabOrderQuestion labOrderQuestion = questionState.getEntity();

        if (questionState.isEntityCreated() || validFrom.isAfter(labOrderQuestion.getValidFrom())) {
            questionState.assignIfDifferent(answer, labOrderQuestion.getAnswer(), labOrderQuestion::setAnswer);
        }
        questionState.saveEntityOrAuditLogIfRequired(labOrderQuestionRepo, labOrderQuestionAuditRepo);
    }

    private Question getOrCreateQuestion(String question) {
        return questionRepo.findByQuestion(question)
                .orElseGet(() -> questionRepo.save(new Question(question)));
    }
}
