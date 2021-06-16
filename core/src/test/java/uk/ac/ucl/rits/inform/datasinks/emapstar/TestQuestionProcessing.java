package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestQuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestQuestionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabSampleQuestion;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Testing functionality in relation to question and answers, e.g. for lab samples or consultation requests.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
class TestQuestionProcessing extends MessageProcessingBase {
    private LabOrderMsg labOrderMsg;
    private final String coPathTemplate = "co_path/%s.yaml";
    private final String coPathSampleNumber = "UH20-4444";
    private final Instant messageTime = Instant.parse("2020-11-09T15:04:45Z");
    private ConsultRequest consultReqMsg;

    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    LabSampleQuestionRepository labSampleQuestionRepository;
    @Autowired
    LabSampleQuestionAuditRepository labSampleQuestionAuditRepository;
    @Autowired
    ConsultationRequestQuestionRepository consultationRequestQuestionRepo;
    @Autowired
    ConsultationRequestQuestionAuditRepository consultationRequestQuestionAuditRepo;

    @BeforeEach
    void setUp() throws IOException {
        labOrderMsg = messageFactory.buildLabOrderOverridingDefaults(
                String.format(coPathTemplate, "orm_defaults"), String.format(coPathTemplate, "orm_o01_questions")
        );
        consultReqMsg = messageFactory.getConsult("notes.yaml");
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
        assertEquals(3, labSampleQuestionRepository.count());
        assertEquals(0, labSampleQuestionAuditRepository.count());
    }

    /**
     * Nothing in database, and 3 questions in message.
     * Should create 3 questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testLabOrderQuestionsAdded() throws Exception {
        processSingleMessage(labOrderMsg);
        assertEquals(3, labSampleQuestionRepository.count());
        assertEquals(0, labSampleQuestionAuditRepository.count());
    }

    @Test
    void testLabQuestionAnswerUpdatedIfNewer() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process later message with updated answer
        labOrderMsg.setStatusChangeTime(messageTime.plusSeconds(60));
        String clinicalQuestion = "Clinical Details:";
        String newClinicalAnswer = "very sleepy";
        labOrderMsg.getQuestions().put(clinicalQuestion, newClinicalAnswer);
        processSingleMessage(labOrderMsg);

        assertEquals(3, labSampleQuestionRepository.count());
        assertEquals(1, labSampleQuestionAuditRepository.count());
        LabSampleQuestion labSampleQuestion = labSampleQuestionRepository.findByQuestionIdQuestion(clinicalQuestion).orElseThrow();
        assertEquals(newClinicalAnswer, labSampleQuestion.getAnswer());
    }

    @Test
    void testLabQuestionAnswerNotUpdatedIfOlder() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process earlier message with updated answer
        labOrderMsg.setStatusChangeTime(messageTime.minusSeconds(60));
        String clinicalQuestion = "Clinical Details:";
        String newClinicalAnswer = "very sleepy";
        labOrderMsg.getQuestions().put(clinicalQuestion, newClinicalAnswer);
        processSingleMessage(labOrderMsg);

        LabSampleQuestion labSampleQuestion = labSampleQuestionRepository.findByQuestionIdQuestion(clinicalQuestion).orElseThrow();
        assertNotEquals(newClinicalAnswer, labSampleQuestion.getAnswer());
    }

    /**
     * Delete lab order given message, but lab questions don't already exist.
     * Should still create lab orders
     * @throws Exception shouldn't happen
     */
    @Test
    void testLabQuestionDeleteDoesntExist() throws Exception {
        labOrderMsg.setEpicCareOrderNumber(InterchangeValue.deleteFromValue(coPathSampleNumber));
        processSingleMessage(labOrderMsg);
        assertEquals(0, labSampleQuestionRepository.count());
    }

    /**
     * Create order with 3 questions,
     * then send same order with later time and delete order - should delete all lab questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testLabQuestionsNotDeleted() throws Exception {
        // process original message
        processSingleMessage(labOrderMsg);
        // process later message with delete order
        labOrderMsg.setEpicCareOrderNumber(InterchangeValue.deleteFromValue(coPathSampleNumber));
        labOrderMsg.setStatusChangeTime(messageTime.plusSeconds(60));
        processSingleMessage(labOrderMsg);

        assertEquals(3, labSampleQuestionRepository.count());
    }

    /**
     * Nothing in database, and 3 questions in message.
     * Should create 3 questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testConsultationRequestQuestionsAdded() throws Exception {
        processSingleMessage(labOrderMsg);
        assertEquals(3, labSampleQuestionRepository.count());
        assertEquals(0, labSampleQuestionAuditRepository.count());
    }
}
