package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.QuestionRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases to assert correct functionality of consultation request handling in EMAP Core.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public class TestConsultProcessing extends MessageProcessingBase {
    @Autowired
    MrnRepository mrnRepository;
    @Autowired
    ConsultationRequestRepository consultRequestRepo;
    @Autowired
    ConsultationTypeRepository consultTypeRepo;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    QuestionRepository questionRepository;

    private ConsultRequest minimalConsult;
    private ConsultRequest cancelledConsult;
    private ConsultRequest closedAtDischargeConsult;
    private ConsultRequest notesConsult;
    private static String FRAILTY_MRN = "40800000";
    private static String FRAILTY_VISIT_ID = "123412341234";
    private static Long FRAILTY_CONSULT_ID = Long.valueOf(1234521112);
    private static Instant FRAILTY_REQ_TIME = Instant.parse("2013-02-12T11:55:00Z");
    private static Instant FRAILTY_STAT_CHANGE_TIME = Instant.parse( "2013-02-12T12:00:00Z");
    private static String FRAILTY_CONSULTATION_TYPE = "CON255";
    private static String FRAILTY_NOTE = "Admitted with delirium vs cognitive decline\nLives alone";

    @BeforeEach
    private void setUp() throws IOException {
        minimalConsult = messageFactory.getConsult("minimal.yaml");
        cancelledConsult = messageFactory.getConsult("cancelled.yaml");
        closedAtDischargeConsult = messageFactory.getConsult("closed_at_discharge.yaml");
        notesConsult = messageFactory.getConsult("notes.yaml");
    }

    /**
     * Given that no MRNS or hospital visits exist in the database
     * When a consult message is processed
     * Then minimal MRN and hospital visit should be created
     */
    @Test
    void testMinimalMrnAndHospitalVisitCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals(FRAILTY_MRN, mrns.get(0).getMrn());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
    }

    /**
     * Given that MRNs and hospital visits exist in the database
     * When a consult message is processed with existing mrn and visit number
     * Then minimal MRN and hospital visit should not be created
     */
    @Test
    void testMinimalMrnAndHospitalVisitNotCreated() throws EmapOperationMessageProcessingException{
        processSingleMessage(minimalConsult);

        List mrns = getAllMrns();
        assertEquals(1, mrns.size());
        List visits = getAllEntities(hospitalVisitRepository);
        assertEquals(1, visits.size());

        processSingleMessage(minimalConsult);
        mrns = getAllMrns();
        assertEquals(1, mrns.size());
        visits = getAllEntities(hospitalVisitRepository);
        assertEquals(1, visits.size());
    }

    /**
     * Given that no consult types exist in the database
     * When a consult message is processed
     * A new minimal consult type (only populating the code and source system, leaving the name column empty for
     * metadata hoovering) should be created
     */
    @Test
    void testMinimalConsultTypeCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);
        ConsultationType crType = consultTypeRepo.findByCode(FRAILTY_CONSULTATION_TYPE).orElseThrow();

        assertEquals(FRAILTY_REQ_TIME, crType.getValidFrom());
        assertNull(crType.getName());
    }

    /**
     * Given that no consults exist in the database
     * When a consult message is processed
     * A new consult should be created (in addition to PK and FKs should store internalConsultId, requestedDateTime, storedFrom, validFrom)
     */
    @Test
    void testCreateNewConsult() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);

        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();

        assertNull(cRequest.getComments());
        assertNotNull(cRequest.getConsultationRequestId());
        assertEquals(FRAILTY_CONSULT_ID, cRequest.getConsultId());
        assertNotNull(cRequest.getValidFrom());
        assertNotNull(cRequest.getStoredFrom());
        assertEquals(FRAILTY_REQ_TIME, cRequest.getRequestedDateTime());
        assertEquals(FRAILTY_STAT_CHANGE_TIME, cRequest.getStatusChangeTime());
    }

    /**
     * Given that no questions and consult questions exist in the database
     * When a consult message is processed with 3 questions
     * Then 3 questions should be created and linked to 3 consult questions for the answers to the questions
     */
    @Test
    void testCreateConsultWithQuestions() throws EmapOperationMessageProcessingException{
        processSingleMessage(notesConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertEquals(3, questionRepository.count());
    }

    /**
     * Given that no consults exist in the database
     * When a consult message is processed with notes
     * Then a new consult should be created with the notes being saved in the comments
     */
    @Test
    void testCreateConsultWithNotes() throws EmapOperationMessageProcessingException {
        processSingleMessage(notesConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertNotNull(cRequest.getComments());
        assertEquals(FRAILTY_NOTE, cRequest.getComments());
    }

    /**
     * Given that the minimal consult has already been processed
     * When a later consult message with cancel=true with the same epicConsultId and consultationType is processed
     * Then consult should have a cancelled state or similar set to true and the storedFrom and validFrom fields update
     */
    @Test
    void testLaterMessageCancelRequest() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertFalse(cRequest.getCancelled());

        processSingleMessage(cancelledConsult);
        cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertTrue(cRequest.getCancelled());
    }

    /**
     * Given that the minimal consult has already been processed
     * When a later consult message with closedDueToDischarge=true with the same epicConsultId and consultationType is processed
     * The consult should have a closedOnDischarge state or similar set to true and the storedFrom and validFrom fields update
     */
    @Test
    void testLaterMessageClosedDueToDischarge() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertFalse(cRequest.getClosedDueToDischarge());

        processSingleMessage(closedAtDischargeConsult);
        cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertTrue(cRequest.getClosedDueToDischarge());
    }

    /**
     * Given that a minimal consult has already been processed
     * When an earlier consult message with different data is processed
     * The consult entity in question should not be updated
     */
    @Test
    void testEarlierMessageNoCommentUpdate() throws EmapOperationMessageProcessingException {
        String great_note = "Great note";
        minimalConsult.setNotes(InterchangeValue.buildFromHl7(great_note));
        processSingleMessage(minimalConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertEquals(cRequest.getComments(), great_note);

        String great_note_2 = "bla bla bla";
        minimalConsult.setStatusChangeTime(minimalConsult.getStatusChangeTime().minusSeconds(60));
        minimalConsult.setNotes(InterchangeValue.buildFromHl7(great_note_2));
        processSingleMessage(minimalConsult);
        cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertEquals(cRequest.getComments(), great_note);
    }

    /**
     * Given that a minimal consult has already been processed
     * When an earlier consult message with cancellation flag active is sent
     * The consult entity in question should not be updated
     */
    @Test
    void testEarlierMessageNoCancelUpdate() throws EmapOperationMessageProcessingException {
        String great_note = "Great note";
        minimalConsult.setNotes(InterchangeValue.buildFromHl7(great_note));
        processSingleMessage(minimalConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertEquals(cRequest.getComments(), great_note);

        cancelledConsult.setStatusChangeTime(minimalConsult.getStatusChangeTime().minusSeconds(60));
        processSingleMessage(cancelledConsult);
        cRequest = consultRequestRepo.findByConsultId(FRAILTY_CONSULT_ID).orElseThrow();
        assertFalse(cRequest.getCancelled());
    }

}
