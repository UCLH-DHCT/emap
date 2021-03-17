package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderQuestionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderQuestion;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TestQuestionProcessing extends MessageProcessingBase {
    private LabOrderMsg labOrderMsg;
    private final String coPathTemplate = "co_path/%s.yaml";
    private final String coPathOrderNumber = "10004000";
    private final Instant messageTime = Instant.parse("2020-11-09T15:04:45Z");

    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    LabOrderQuestionRepository labOrderQuestionRepository;
    @Autowired
    LabOrderQuestionAuditRepository labOrderQuestionAuditRepository;


    @BeforeEach
    void setUp() {
        labOrderMsg = messageFactory.buildLabOrderOverridingDefaults(
                String.format(coPathTemplate, "orm_defaults"), String.format(coPathTemplate, "orm_o01_questions")
        );
    }

    /**
     * Nothing in database, and 3 questions in message.
     * Should create 3 questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testQuestionsAdded() throws Exception {
        processSingleMessage(labOrderMsg);
        assertEquals(3, questionRepository.count());
        assertEquals(3, labOrderQuestionRepository.count());
        assertEquals(0, labOrderQuestionAuditRepository.count());
    }

    /**
     * Nothing in database, and 3 questions in message.
     * Should create 3 questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testLabOrderQuestionsAdded() throws Exception {
        processSingleMessage(labOrderMsg);
        assertEquals(3, labOrderQuestionRepository.count());
        assertEquals(0, labOrderQuestionAuditRepository.count());
    }

    @Test
    void testLabQuestionAnswerUpdatedIfNewer() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process later message with updated answer
        labOrderMsg.setStatusChangeTime(messageTime.plusSeconds(60));
        String clinicalQuestion = "Clinical Details:";
        String newClinicalAnswer = "very sleepy";
        Pair<String, String> questionAndAnswer = labOrderMsg.getQuestions()
                .stream()
                .filter(p -> clinicalQuestion.equals(p.left))
                .findFirst().orElseThrow();
        questionAndAnswer.setValue(newClinicalAnswer);
        processSingleMessage(labOrderMsg);

        assertEquals(3, labOrderQuestionRepository.count());
        assertEquals(1, labOrderQuestionAuditRepository.count());
        LabOrderQuestion labOrderQuestion = labOrderQuestionRepository.findByQuestionIdQuestion(clinicalQuestion).orElseThrow();
        assertEquals(newClinicalAnswer, labOrderQuestion.getAnswer());
    }

    @Test
    void testLabQuestionAnswerNotUpdatedIfOlder() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process earlier message with updated answer
        labOrderMsg.setStatusChangeTime(messageTime.minusSeconds(60));
        String clinicalQuestion = "Clinical Details:";
        String newClinicalAnswer = "very sleepy";
        Pair<String, String> questionAndAnswer = labOrderMsg.getQuestions()
                .stream()
                .filter(p -> clinicalQuestion.equals(p.left))
                .findFirst().orElseThrow();
        questionAndAnswer.setValue(newClinicalAnswer);
        processSingleMessage(labOrderMsg);

        LabOrderQuestion labOrderQuestion = labOrderQuestionRepository.findByQuestionIdQuestion(clinicalQuestion).orElseThrow();
        assertNotEquals(newClinicalAnswer, labOrderQuestion.getAnswer());
    }

    /**
     * Delete lab order given message, but lab questions don't already exist.
     * Should not create any lab orders.
     * @throws Exception shouldn't happen
     */
    @Test
    void testLabQuestionDeleteDoesntExist() throws Exception {
        labOrderMsg.setEpicCareOrderNumber(InterchangeValue.deleteFromValue(coPathOrderNumber));
        processSingleMessage(labOrderMsg);
        assertEquals(0, labOrderQuestionRepository.count());
    }

    /**
     * Create order with 3 questions,
     * then send same order with later time and delete order - should delete all lab questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testDeleteLabQuestion() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process later message with delete order
        labOrderMsg.setEpicCareOrderNumber(InterchangeValue.deleteFromValue(coPathOrderNumber));
        labOrderMsg.setStatusChangeTime(messageTime.plusSeconds(60));
        processSingleMessage(labOrderMsg);

        assertEquals(0, labOrderQuestionRepository.count());
        assertEquals(3, labOrderQuestionAuditRepository.count());
    }

    /**
     * Create order with 3 questions,
     * then send same order with earlier time and delete order - should not delete the questions.
     * @throws Exception shouldn't happen
     */
    @Test
    void testDeleteLabQuestionIsOlderThanDb() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process earlier message with delete order
        labOrderMsg.setEpicCareOrderNumber(InterchangeValue.deleteFromValue(coPathOrderNumber));
        labOrderMsg.setStatusChangeTime(messageTime.minusSeconds(60));
        processSingleMessage(labOrderMsg);

        assertEquals(3, labOrderQuestionRepository.count());
        assertEquals(0, labOrderQuestionAuditRepository.count());
    }
}
