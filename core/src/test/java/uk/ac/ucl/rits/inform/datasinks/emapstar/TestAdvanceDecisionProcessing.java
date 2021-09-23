package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.*;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvanceDecisionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvanceDecisionTypeRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecision;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecisionType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.AdvanceDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases to assert correct processing of AdvanceDecisionMessages.
 * @author Anika Cawthorn
 */
public class TestAdvanceDecisionProcessing extends MessageProcessingBase {
    @Autowired
    AdvanceDecisionRepository advanceDecisionRepo;
    @Autowired
    AdvanceDecisionTypeRepository advanceDecisionTypeRepo;
    @Autowired
    MrnRepository mrnRepository;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    QuestionRepository questionRepository;

    private AdvanceDecisionMessage minimalNoQuestions;
    private AdvanceDecisionMessage minimalWithQuestions;
    private AdvanceDecisionMessage closedAtDischarge;
    private AdvanceDecisionMessage cancelled;

    private static String ADVANCE_DECISION_MRN = "40800000";
    private static String ADVANCE_DECISION_CARE_CODE = "COD4";
    private static Instant ADVANCE_DECISION_REQ_TIME =  Instant.parse("2013-02-12T11:55:00Z");
    private static Instant ADVANCE_DECISION_STAT_CHANGE_TIME =  Instant.parse("2013-02-14T09:00:00Z");
    private static Long ADVANCE_DECISION_INTERNAL_ID = 1234521112L;

    @BeforeEach
    private void setUp() throws IOException {
        minimalNoQuestions = messageFactory.getAdvanceDecision("minimal.yaml");
        minimalWithQuestions = messageFactory.getAdvanceDecision("new_with_questions.yaml");
        closedAtDischarge = messageFactory.getAdvanceDecision("closed_at_discharge.yaml");
        cancelled = messageFactory.getAdvanceDecision("cancelled.yaml");
    }

    /**
     *  Given that no MRNS or hospital visits exist in the database
     *  When a new AdvanceDecisionMessage without questions arrives
     *  Then a minimal HospitalVisit, Mrn and AdvancedDecision should be created
     */
    @Test
    void testMinimalAdvancedDecisionCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());

        assertEquals(ADVANCE_DECISION_MRN, mrns.get(0).getMrn());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
    }

    /**
     * Given that MRNs and hospital visits exist in the database
     * When an AdvanceDecisionMessage is processed with existing mrn and visit number
     * Then minimal MRN and hospital visit are not created
     */
    @Test
    void testMinimalMrnAndHospitalVisitNotCreated() throws EmapOperationMessageProcessingException{
        processSingleMessage(minimalNoQuestions);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        List<HospitalVisit> visits = getAllEntities(hospitalVisitRepository);
        assertEquals(1, visits.size());

        processSingleMessage(minimalNoQuestions);
        mrns = getAllMrns();
        assertEquals(1, mrns.size());
        visits = getAllEntities(hospitalVisitRepository);
        assertEquals(1, visits.size());
    }

    /**
     * Given that no advance decision type exist in the database
     * When an AdvanceDecisionMessage is processed
     * Then A new minimal AdvancedDecicisionType should be created
     */
    @Test
    void testMinimalAdvancedDecisionTypeCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);
        AdvanceDecisionType adType = advanceDecisionTypeRepo.findByCareCode(ADVANCE_DECISION_CARE_CODE).orElseThrow();

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

        AdvanceDecision advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();

        assertNotNull(advanceDecision.getValidFrom());
        assertNotNull(advanceDecision.getStoredFrom());
        assertEquals(ADVANCE_DECISION_REQ_TIME, advanceDecision.getRequestedDatetime());
        assertEquals(ADVANCE_DECISION_STAT_CHANGE_TIME, advanceDecision.getStatusChangeTime());
    }

    /**
     *  Given that no AdvanceDecision exist
     *  When a new AdvanceDecisionMessage with questions arrives
     *  Then a new AdvanceDecision and the related questions should be created
     */
    @Test
    void testAdvancedDecisionWithQuestionsCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalWithQuestions);
        AdvanceDecision advanceDecisionRequest = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertEquals(3, questionRepository.count());
    }

    /**
     * Given that the minimal AdvanceDecision has already been processed
     * When a later AdvanceDecisionMessage with cancel=true with the same EPIC id and advancedDecisionType is processed
     * Then the existing AdvanceDecision has after processing a cancelled state set to true and the storedFrom and
     *   validFrom fields are updated
     */
    @Test
    void testLaterMessageCancelRequest() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        AdvanceDecision advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertFalse(advanceDecision.getCancelled());

        cancelled.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().plusSeconds(60));
        processSingleMessage(cancelled);
        advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertTrue(advanceDecision.getCancelled());
        assertEquals(ADVANCE_DECISION_STAT_CHANGE_TIME.plusSeconds(60), advanceDecision.getStatusChangeTime());
    }

    /**
     * Given that minimal AdvancedDecision has already been processed
     * When a later AdvancedDecision with closedDueToDischarge=true with the same epicConsultId and
     *   advancedDecisionType is processed
     * Then the existing AdvancedDecision has after processing a closedOnDischarge set to true and the storedFrom and
     *   validFrom fields are updated
     */
    @Test
    void testLaterMessageClosedDueToDischarge() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);
        AdvanceDecision advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertFalse(advanceDecision.getClosedDueToDischarge());

        closedAtDischarge.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().plusSeconds(60));
        processSingleMessage(closedAtDischarge);
        advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertTrue(advanceDecision.getClosedDueToDischarge());
        assertEquals(ADVANCE_DECISION_STAT_CHANGE_TIME.plusSeconds(60), advanceDecision.getStatusChangeTime());
    }

    /**
     * Given that an AdvancedDecision has already been processed
     * When an earlier AdvancedDecisionMessage with cancellation flag active is sent
     * Then the existing AdvancedDecision is not updated
     */
    @Test
    void testEarlierMessageNoCancelUpdate() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        AdvanceDecision advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertFalse(advanceDecision.getCancelled());

        cancelled.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().minusSeconds(60));
        processSingleMessage(cancelled);
        advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertFalse(advanceDecision.getCancelled());
    }

    /**
     * Given that an AdvanceDecision has already been processed
     * When an earlier AdvanceDecisionMessage with closedDueToDischarge flag active is sent
     * Then the existing AdvanceDecision is not updated
     */
    @Test
    void testEarlierMessageNoClosedDueToDischargeUpdate() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);
        AdvanceDecision advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertFalse(advanceDecision.getClosedDueToDischarge());

        closedAtDischarge.setStatusChangeTime(minimalNoQuestions.getStatusChangeTime().minusSeconds(60));
        processSingleMessage(closedAtDischarge);
        advanceDecision = advanceDecisionRepo.findByInternalId(ADVANCE_DECISION_INTERNAL_ID).orElseThrow();
        assertFalse(advanceDecision.getClosedDueToDischarge());
    }
}
