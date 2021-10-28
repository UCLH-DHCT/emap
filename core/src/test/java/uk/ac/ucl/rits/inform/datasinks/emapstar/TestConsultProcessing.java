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
import uk.ac.ucl.rits.inform.interchange.ConsultMetadata;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private ConsultMetadata frailtyMetadata;
    private static String FRAILTY_MRN = "40800000";
    private static Long FRAILTY_CONSULT_ID = 1234521112L;
    private static Instant FRAILTY_REQ_TIME = Instant.parse("2013-02-12T11:55:00Z");
    private static Instant FRAILTY_UPDATE_TIME = Instant.parse("2013-02-12T12:00:00Z");
    private static Instant FRAILTY_STAT_CHANGE_TIME = Instant.parse("2013-02-12T12:00:00Z");
    private static String FRAILTY_CONSULTATION_TYPE = "CON255";
    private static String FRAILTY_NOTE = "Admitted with delirium vs cognitive decline\nLives alone";
    private static String FRAILTY_NAME = "Inpatient Consult to Acute Frailty Team";

    @BeforeEach
    private void setUp() throws IOException {
        minimalConsult = messageFactory.getConsult("minimal.yaml");
        cancelledConsult = messageFactory.getConsult("cancelled.yaml");
        closedAtDischargeConsult = messageFactory.getConsult("closed_at_discharge.yaml");
        notesConsult = messageFactory.getConsult("notes.yaml");
        frailtyMetadata = messageFactory.getConsultMetadata("con255.yaml");
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
    void testMinimalMrnAndHospitalVisitNotCreated() throws EmapOperationMessageProcessingException {
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

        assertEquals(FRAILTY_UPDATE_TIME, crType.getValidFrom());
        assertNull(crType.getName());
    }

    /**
     * Given that no consults exist in the database
     * When a consult message is processed
     * A new consult should be created (in addition to PK and FKs should store internalConsultId, getScheduledDateTime, storedFrom, validFrom)
     */
    @Test
    void testCreateNewConsult() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);

        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();

        assertNull(cRequest.getComments());
        assertNotEquals(0, cRequest.getConsultationRequestId());
        assertEquals(FRAILTY_CONSULT_ID, cRequest.getInternalId());
        assertNotNull(cRequest.getValidFrom());
        assertNotNull(cRequest.getStoredFrom());
        assertEquals(FRAILTY_REQ_TIME, cRequest.getScheduledDatetime());
        assertEquals(FRAILTY_STAT_CHANGE_TIME, cRequest.getStatusChangeTime());
    }

    /**
     * Given that no questions and consult questions exist in the database
     * When a consult message is processed with 3 questions
     * Then 3 questions should be created and linked to 3 consult questions for the answers to the questions
     */
    @Test
    void testCreateConsultWithQuestions() throws EmapOperationMessageProcessingException {
        processSingleMessage(notesConsult);
        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
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
        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
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
        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
        assertFalse(cRequest.getCancelled());

        processSingleMessage(cancelledConsult);
        cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
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
        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
        assertFalse(cRequest.getClosedDueToDischarge());

        processSingleMessage(closedAtDischargeConsult);
        cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
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
        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
        assertEquals(cRequest.getComments(), great_note);

        String great_note_2 = "bla bla bla";
        minimalConsult.setStatusChangeTime(minimalConsult.getStatusChangeTime().minusSeconds(60));
        minimalConsult.setNotes(InterchangeValue.buildFromHl7(great_note_2));
        processSingleMessage(minimalConsult);
        cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
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
        ConsultationRequest cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
        assertEquals(cRequest.getComments(), great_note);

        cancelledConsult.setStatusChangeTime(minimalConsult.getStatusChangeTime().minusSeconds(60));
        processSingleMessage(cancelledConsult);
        cRequest = consultRequestRepo.findByInternalId(FRAILTY_CONSULT_ID).orElseThrow();
        assertFalse(cRequest.getCancelled());
    }

    /**
     * Given that no consult types exist in database
     * when consult metadata is processed
     * new consult type be created
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testNewTypeCreatedFromMetadata() throws EmapOperationMessageProcessingException {
        processSingleMessage(frailtyMetadata);
        ConsultationType crType = consultTypeRepo.findByCode(FRAILTY_CONSULTATION_TYPE).orElseThrow();

        assertEquals(FRAILTY_NAME, crType.getName());
    }

    /**
     * Given frailty consult type exists from request message
     * When the metadata is processed with an update time before the request message
     * Then the frailty metadata name should still be added to the type
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testMinimalConsultTypeUpdatedWithMetadata() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);
        frailtyMetadata.setLastUpdatedDate(FRAILTY_REQ_TIME.minusSeconds(1));
        processSingleMessage(frailtyMetadata);

        ConsultationType crType = consultTypeRepo.findByCode(FRAILTY_CONSULTATION_TYPE).orElseThrow();

        assertEquals(FRAILTY_NAME, crType.getName());
    }

    /**
     * Given that frailty type has been populated from metadata
     * When older metadata for the same type is processed
     * Then the older metadata message should have no effect
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testTypeNotUpdatedWithOlderMetadata() throws EmapOperationMessageProcessingException {
        Instant originalUpdateTime = frailtyMetadata.getLastUpdatedDate();
        processSingleMessage(frailtyMetadata);
        ConsultMetadata olderMetadata = frailtyMetadata;
        olderMetadata.setLastUpdatedDate(originalUpdateTime.minusSeconds(1));
        olderMetadata.setName("Crazy name that was clearly wrong so changed");

        ConsultationType crType = consultTypeRepo.findByCode(FRAILTY_CONSULTATION_TYPE).orElseThrow();

        assertEquals(FRAILTY_NAME, crType.getName());
        assertEquals(originalUpdateTime, crType.getValidFrom());
    }

}
