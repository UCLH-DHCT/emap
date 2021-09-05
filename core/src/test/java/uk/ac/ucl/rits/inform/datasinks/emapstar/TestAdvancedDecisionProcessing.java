package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.*;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.AdvancedDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases to assert correct processing of Advanced Decision Messages.
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

    @BeforeEach
    private void setUp() throws IOException {
        minimalNoQuestions = messageFactory.getAdvancedDecision("new_no_questions.yaml");
        minimalWithQuestions = messageFactory.getAdvancedDecision("new_with_questions.yaml");
        closedAtDischarge = messageFactory.getAdvancedDecision("discharge_cancel.yaml");
        cancelled = messageFactory.getAdvancedDecision("user_cancel.yaml");
    }

    /**
     *  Given that no MRNS or hospital visits exist in the database
     *  When a new AdvancedDecisionMessage arrives
     *  Then a minimal HospitalVisit, Mrn and AdvancedDecision should be created
     */
    @Test
    void testMinimalAdvancedDecisionCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalNoQuestions);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
    }
}
