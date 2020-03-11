/**
 * 
 */
package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.EncounterRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientFactRepository;
import uk.ac.ucl.rits.inform.informdb.Fact;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Test cases that take a stream of Emap Interchange messages as an input,
 * and inspect the resultant changes to Emap-Star.
 *
 * @author Jeremy Stein
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class MessageStreamTestCase {
    @Autowired
    protected InformDbOperations dbOps;
    @Autowired
    protected EncounterRepository encounterRepo;
    @Autowired
    protected MrnRepository mrnRepo;
    @Autowired
    protected PatientFactRepository patientFactRepo;

    protected List<EmapOperationMessage> messageStream = new ArrayList<>();

    @Before
    @Transactional
    public void setup() throws EmapOperationMessageProcessingException {
        for (EmapOperationMessage msg : messageStream) {
            processSingleMessage(msg);
        }
    }

    @Transactional
    protected void processSingleMessage(EmapOperationMessage msg) throws EmapOperationMessageProcessingException {
        msg.processMessage(dbOps);
    }

    /**
     * Check the parent/child relationship has been established in both directions.
     * @param parent the expected parent
     * @param child the expected child
     */
    protected void assertIsParentOfChild(PatientFact parent, PatientFact child) {
        assertTrue(child.getParentFact() == parent);
        assertTrue(parent.getChildFacts().stream().anyMatch(cf -> cf == child));
    }

    /**
     * Check parent/child relationship for a list of siblings.
     * @param parent the expected parent of all the children
     * @param children the children all of which have the expected parent
     */
    protected void assertIsParentOfChildren(PatientFact parent, List<PatientFact> children) {
        for (PatientFact child : children) {
            assertIsParentOfChild(parent, child);
        }
    }
    
}
