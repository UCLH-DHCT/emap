package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.informdb.questions.Question;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
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
    private final Instant messageTime_cRequest = Instant.parse("2013-02-12T12:00:00Z");
    private ConsultRequest consultReqMsg;

    @Autowired
    QuestionRepository questionRepo;
    @Autowired
    QuestionAuditRepository questionAuditRepo;
    @Autowired
    LabSampleRepository labSampleRepo;
    @Autowired
    ConsultationRequestRepository consultationRequestRepo;

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
        assertEquals(3, questionRepo.count());
        assertEquals(0, questionAuditRepo.count());
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
        LabSample sample = labSampleRepo.findByMrnIdAndExternalLabNumber(labOrderMsg.getMrn(),
                labOrderMsg.getLabSpecimenNumber());

        assertEquals(3, questionRepo.count());
        assertEquals(1, questionAuditRepo.count());
        Question question = questionRepo.findByQuestionAndParentTableIdentifier(clinicalQuestion,
                sample.getExternalLabNumber()).orElseThrow();
        assertEquals(newClinicalAnswer, question.getAnswer());
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

        LabSample sample = labSampleRepo.findByMrnIdAndExternalLabNumber(labOrderMsg.getMrn(),
                labOrderMsg.getLabSpecimenNumber());

        Question question= questionRepo.findByQuestionAndParentTableIdentifier(clinicalQuestion,
                sample.getExternalLabNumber()).orElseThrow();
        assertNotEquals(newClinicalAnswer, question.getAnswer());
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
        assertEquals(0, questionRepo.count());
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

        assertEquals(3, questionRepo.count());
    }

    /**
     * Nothing in database, and 3 questions in message.
     * Should create 3 questions
     * @throws Exception shouldn't happen
     */
    @Test
    void testConsultationRequestQuestionsAdded() throws Exception {
        processSingleMessage(consultReqMsg);
        assertEquals(3, questionRepo.count());
        assertEquals(0, questionAuditRepo.count());
    }

    /**
     * Once consultation request question exists, update answer if newer message processed.
     * @throws Exception shouldn't happen
     */
    @Test
    void testConsultationRequestQuestionAnswerUpdatedIfNewer() throws Exception {
        // process original message
        String clinicalQuestion = "Did you contact the team?";
        String newClinicalAnswer = "yes";

        processSingleMessage(consultReqMsg);
        ConsultationRequest request =
        Question question = questionRepo.findByQuestionAndParentTableIdentifier(clinicalQuestion).orElseThrow();

        // process later message with updated answer
        consultReqMsg.setRequestedDateTime(messageTime_cRequest.plusSeconds(60));
        consultReqMsg.getQuestions().put(clinicalQuestion, newClinicalAnswer);

        processSingleMessage(consultReqMsg);

        assertEquals(3, questionRepo.count());
        assertEquals(1, questionAuditRepo.count());
        question = questionRepo.findByQuestionAndParentTableIdentifier(clinicalQuestion).orElseThrow();
        assertEquals(newClinicalAnswer, question.getAnswer());
    }

}
