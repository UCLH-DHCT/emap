package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test EPIC Patient Consult Requests parsing
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestConsultRequests extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "ConsultRequest/%s.txt";
    private static final String MRN = "40800000";
    private static final Instant CHANGE_TIME = Instant.parse("2013-02-12T12:00:00Z");
    private static final Instant CANCEL_TIME = Instant.parse("2013-02-12T14:00:00Z");
    private static final Instant CLOSED_TIME = Instant.parse("2013-02-14T09:00:00Z");
    private static final Instant REQUEST_TIME = Instant.parse("2013-02-12T11:55:00Z");
    private static final String EPIC = "EPIC";
    private static final String CONSULT_TYPE = "CON255";
    private static final String VISIT_NUMBER = "123412341234";
    private static final long CONSULT_ID = 1234521112L;


    ConsultRequest getPatientConsult(String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof ConsultRequest))
                .map(o -> (ConsultRequest) o)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Test all data from HL7 is parsed, except for notes and questions which are trickier.
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleInfectionParsed() throws Exception {
        ConsultRequest consult = getPatientConsult("minimal");
        assertEquals(MRN, consult.getMrn());
        assertEquals(EPIC, consult.getSourceSystem());
        assertEquals(CHANGE_TIME, consult.getStatusChangeTime());
        assertEquals(REQUEST_TIME, consult.getScheduledDatetime());
        assertEquals(CONSULT_TYPE, consult.getConsultationType());
        assertEquals(VISIT_NUMBER, consult.getVisitNumber());
        assertEquals(CONSULT_ID, consult.getEpicConsultId());
    }

    /**
     * Ensure that simple questions are parsed correctly.
     * @throws Exception shouldn't happen
     */
    @Test
    void testSimpleQuestionsParsed() throws Exception {
        ConsultRequest consult = getPatientConsult("minimal");
        Map<String, String> questions = consult.getQuestions();
        assertEquals(3, questions.size());
        assertEquals("frail, delirium, ? cognitive decline", questions.get("Reason for Consult?"));
    }

    /**
     * Any notes before a question should be joined to be a comment.
     * @throws Exception shouldn't happen
     */
    @Test
    void testNotesAndQuestionsAreParsed() throws Exception {
        ConsultRequest consult = getPatientConsult("notes");
        InterchangeValue<String> expectedNotes = InterchangeValue.buildFromHl7("Admitted with delirium vs cognitive decline\nLives alone");
        assertEquals(expectedNotes, consult.getNotes());
        assertEquals(3, consult.getQuestions().size());
    }

    /**
     * There shouldn't be multiple consult requests in a single message.
     */
    @Test
    void testMultipleRequestInMessageThrows() {
        assertThrows(Hl7InconsistencyException.class, () -> getPatientConsult("multiple_requests"));
    }


    /**
     * Request has been cancelled by a user.
     * Datetime information should be set as usual, message should be cancelled and not closed at discharge
     * @throws Exception shouldn't happen
     */
    @Test
    void testCancelledOrder() throws Exception {
        ConsultRequest consult = getPatientConsult("cancelled");
        assertEquals(REQUEST_TIME, consult.getScheduledDatetime());
        assertEquals(CANCEL_TIME, consult.getStatusChangeTime());
        assertTrue(consult.isCancelled());
        assertFalse(consult.isClosedDueToDischarge());
    }

    /**
     * Request automatically closed by DISCHAUTO.
     * Datetime information should be set as usual, message should be closed at discharge, not cancelled.
     * @throws Exception shouldn't happen
     */
    @Test
    void testClosedAtDischarge() throws Exception {
        ConsultRequest consult = getPatientConsult("closed_at_discharge");
        assertEquals(REQUEST_TIME, consult.getScheduledDatetime());
        assertEquals(CLOSED_TIME, consult.getStatusChangeTime());
        assertFalse(consult.isCancelled());
        assertTrue(consult.isClosedDueToDischarge());

    }

}
