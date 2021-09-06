package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.*;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionTypeRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecision;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecisionType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.AdvancedDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases to assert correct processing of AdvancedDecisionMessages.
 * @author Anika Cawthorn
 */
public class TestAdvancedDecisionProcessing extends MessageProcessingBase {
    @Autowired
    AdvancedDecisionRepository advancedDecisionRepo;
    @Autowired
    AdvancedDecisionTypeRepository advancedDecisionTypeRepo;
    @Autowired
    MrnRepository mrnRepository;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    QuestionRepository questionRepository;

    private AdvancedDecisionMessage minimalNoQuestions;
    private AdvancedDecisionMessage minimalWithQuestions;
    private AdvancedDecisionMessage closedAtDischarge;
    private AdvancedDecisionMessage cancelled;

    private static String ADVANCED_DECISION_MRN = "40800000";
    private static String ADVANCED_DECISION_CARE_CODE = "COD4";
    private static Instant ADVANCED_DECISION_REQ_TIME =  Instant.parse("2013-02-12T11:55:00Z");
    private static Instant ADVANCED_DECISION_STAT_CHANGE_TIME =  Instant.parse("2013-02-14T14:09:00Z");
    private static Long ADVANCED_DECISION_NUMBER = Long.valueOf("1234521112");

    @BeforeEach
    private void setUp() throws IOException {
        minimalNoQuestions = messageFactory.getAdvancedDecision("new_no_questions.yaml");
        minimalWithQuestions = messageFactory.getAdvancedDecision("new_with_questions.yaml");
        closedAtDischarge = messageFactory.getAdvancedDecision("discharge_cancel.yaml");
        cancelled = messageFactory.getAdvancedDecision("user_cancel.yaml");
    }

    /**
     *  Given that no MRNS or hospital visits exist in the database
     *  When a new AdvancedDecisionMessage without questions arrives
     *  Then a minimal HospitalVisit, Mrn and AdvancedDecision should be created
     */
    @Test
    void testMinimalAdvancedDecisionCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());

        assertEquals(ADVANCED_DECISION_MRN, mrns.get(0).getMrn());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
    }

    /**
     * Given that MRNs and hospital visits exist in the database
     * When an AdvancedDecisionMessage is processed with existing mrn and visit number
     * Then minimal MRN and hospital visit are not created
     */
    @Test
    void testMinimalMrnAndHospitalVisitNotCreated() throws EmapOperationMessageProcessingException{
        processSingleMessage(minimalNoQuestions);

        List mrns = getAllMrns();
        assertEquals(1, mrns.size());
        List visits = getAllEntities(hospitalVisitRepository);
        assertEquals(1, visits.size());

        processSingleMessage(minimalNoQuestions);
        mrns = getAllMrns();
        assertEquals(1, mrns.size());
        visits = getAllEntities(hospitalVisitRepository);
        assertEquals(1, visits.size());
    }

    /**
     * Given that no advanced decision type exist in the database
     * When an AdvancedDecisionMessage is processed
     * Then A new minimal AdvancedDecicisionType should be created
     */
    @Test
    void testMinimalAdvancedDecisionTypeCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);
        AdvancedDecisionType adType = advancedDecisionTypeRepo.findByCareCode(ADVANCED_DECISION_CARE_CODE).orElseThrow();

        assertEquals(ADVANCED_DECISION_REQ_TIME, adType.getValidFrom());
        assertNotNull(adType.getName());
    }

    /**
     * Given that no AdvancedDecision exist in the database
     * When an AdvancedDecisionMessage is processed
     * Then a new AdvancedDecision is created
     */
    @Test
    void testCreateNewAdvancedDecision() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        AdvancedDecision advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();

        assertNotNull(advancedDecision.getAdvancedDecisionId());
        assertNotNull(advancedDecision.getValidFrom());
        assertNotNull(advancedDecision.getStoredFrom());
        assertEquals(ADVANCED_DECISION_REQ_TIME, advancedDecision.getRequestedDateTime());
        assertEquals(ADVANCED_DECISION_STAT_CHANGE_TIME, advancedDecision.getStatusChangeTime());
    }

    /**
     *  Given that no AdvancedDecision exist
     *  When a new AdvancedDecisionMessage with questions arrives
     *  Then a new AdvancedDecision and the related questions should be created
     */
    @Test
    void testAdvancedDecisionWithQuestionsCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalWithQuestions);
        AdvancedDecision advancedDecisionRequest = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertEquals(3, questionRepository.count());
    }

    /**
     * Given that the minimal AdvancedDecision has already been processed
     * When a later AdvancedDecisionMessage with cancel=true with the same internal id and advancedDecisionType is processed
     * Then the existing AdvancedDecision has after processing a cancelled state set to true and the storedFrom and
     *   validFrom fields are updated
     */
    @Test
    void testLaterMessageCancelRequest() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        AdvancedDecision advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertFalse(advancedDecision.getCancelled());

        cancelled.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().plusSeconds(60));
        processSingleMessage(cancelled);
        advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertTrue(advancedDecision.getCancelled());
    }

    /**
     * Given that minimal AdvancedDecision has already been processed
     * When a later AdvancedDecision with closedDueToDischarge=true with the same internal id and
     *   advancedDecisionType is processed
     * Then the existing AdvancedDecision has after processing a closedOnDischarge set to true and the storedFrom and
     *   validFrom fields are updated
     */
    @Test
    void testLaterMessageClosedDueToDischarge() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);
        AdvancedDecision advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(
                ADVANCED_DECISION_NUMBER).orElseThrow();
        assertFalse(advancedDecision.getClosedDueToDischarge());

        closedAtDischarge.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().plusSeconds(60));
        processSingleMessage(closedAtDischarge);
        advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertTrue(advancedDecision.getClosedDueToDischarge());
    }

    /**
     * Given that an AdvancedDecision has already been processed
     * When an earlier AdvancedDecisionMessage with cancellation flag active is sent
     * Then the existing AdvancedDecision is not updated
     */
    @Test
    void testEarlierMessageNoCancelUpdate() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        AdvancedDecision advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertFalse(advancedDecision.getCancelled());

        cancelled.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().minusSeconds(60));
        processSingleMessage(cancelled);
        advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertFalse(advancedDecision.getCancelled());
    }

    /**
     * Given that an AdvancedDecision has already been processed
     * When an earlier AdvancedDecisionMessage with closedDueToDischarge flag active is sent
     * Then the existing AdvancedDecision is not updated
     */
    @Test
    void testEarlierMessageNoClosedDueToDischargeUpdate() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);
        AdvancedDecision advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(
                ADVANCED_DECISION_NUMBER).orElseThrow();
        assertFalse(advancedDecision.getClosedDueToDischarge());

        closedAtDischarge.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().minusSeconds(60));
        processSingleMessage(closedAtDischarge);
        advancedDecision = advancedDecisionRepo.findByAdvancedDecisionNumber(ADVANCED_DECISION_NUMBER).orElseThrow();
        assertFalse(advancedDecision.getClosedDueToDischarge());
    }
}
