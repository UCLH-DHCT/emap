package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.AdvanceDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test EPIC Patient Advance Decision processing.
 * @author Anika Cawthorn
 */
@ActiveProfiles("test")
@SpringBootTest
public class TestAdvanceDecisions extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "AdvanceDecision/%s.txt";
    private static final String MRN = "40800000";
    private static final Instant CHANGE_TIME = Instant.parse("2013-02-14T09:00:00Z");
    private static final Instant CANCEL_TIME = Instant.parse("2013-02-14T10:00:00Z");
    private static final Instant CLOSED_TIME = Instant.parse("2013-02-14T10:00:00Z");
    private static final Instant REQUEST_TIME = Instant.parse("2013-02-12T11:55:00Z");
    private static final String EPIC = "EPIC";
    private static final String ADVANCED_CARE_CODE = "COD4";
    private static final String VISIT_NUMBER = "123412341234";
    private static final String ADVANCED_DECISION_TYPE_NAME = "FULL ACTIVE TREATMENT";

    AdvanceDecisionMessage getPatientAdvanceDecision(String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof AdvanceDecisionMessage))
                .map(o -> (AdvanceDecisionMessage) o)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Given that nothing has been parsed before
     * When an advance decision message without questions is processed
     * Then all the fields in an advance decision message relevant in the User Data Store are processed
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleAdvancedDecisionProcessed() throws Exception {
        AdvanceDecisionMessage advanceDecision = getPatientAdvanceDecision("minimal");
        assertEquals(MRN, advanceDecision.getMrn());
        assertEquals(EPIC, advanceDecision.getSourceSystem());
        assertEquals(CHANGE_TIME, advanceDecision.getStatusChangeDatetime());
        assertEquals(REQUEST_TIME, advanceDecision.getRequestedDatetime());
        assertEquals(ADVANCED_CARE_CODE, advanceDecision.getAdvanceCareCode());
        assertEquals(VISIT_NUMBER, advanceDecision.getVisitNumber());
        assertEquals(ADVANCED_DECISION_TYPE_NAME, advanceDecision.getAdvanceDecisionTypeName());
    }

    /**
     * Given that nothing has been parsed before
     * When an advance decision message with questions is processed
     * Then all the questions are read out correctly.
     * @throws Exception shouldn't happen
     */
    @Test
    void testSimpleQuestionsParsed() throws Exception {
        AdvanceDecisionMessage advanceDecisionMessage = getPatientAdvanceDecision("minimal_w_questions");
        Map<String, String> questions = advanceDecisionMessage.getQuestions();
        assertEquals(10, questions.size());
        assertEquals("Yes", questions.get("Responsible consultant aware of this DNACPR order?"));
    }

    /**
     * There shouldn't be multiple advance decisions in a single message.
     */
    @Test
    void testMultipleRequestInMessageThrows() {
        assertThrows(Hl7InconsistencyException.class, () -> getPatientAdvanceDecision("multiple_requests"));
    }

    /**
     * Advance decision has been cancelled by a user.
     * Datetime information should be set as usual, message should be cancelled and not closed at discharge
     * @throws Exception shouldn't happen
     */
    @Test
    void testCancelledOrder() throws Exception {
        AdvanceDecisionMessage advanceDecisionMessage = getPatientAdvanceDecision("cancelled");
        assertEquals(REQUEST_TIME, advanceDecisionMessage.getRequestedDatetime());
        assertEquals(CANCEL_TIME, advanceDecisionMessage.getStatusChangeDatetime());
        assertTrue(advanceDecisionMessage.isCancelled());
        assertFalse(advanceDecisionMessage.isClosedDueToDischarge());
    }

    /**
     * Advance decision automatically closed by DISCHAUTO.
     * Datetime information should be set as usual, message should be closed at discharge, not cancelled.
     * @throws Exception shouldn't happen
     */
    @Test
    void testClosedAtDischarge() throws Exception {
        AdvanceDecisionMessage advanceDecisionMessage = getPatientAdvanceDecision("closed_at_discharge");
        assertEquals(REQUEST_TIME, advanceDecisionMessage.getRequestedDatetime());
        assertEquals(CLOSED_TIME, advanceDecisionMessage.getStatusChangeDatetime());
        assertFalse(advanceDecisionMessage.isCancelled());
        assertTrue(advanceDecisionMessage.isClosedDueToDischarge());
    }
}
