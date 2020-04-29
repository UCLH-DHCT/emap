package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.EncounterRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientFactRepository;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.testutils.EmapStarTestUtils;

/**
 * Test cases that take a stream of Emap Interchange messages as an input,
 * and inspect the processing and resultant changes to Emap-Star.
 *
 * @author Jeremy Stein
 */
@SpringJUnitConfig
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@ComponentScan(basePackages = { "uk.ac.ucl.rits.inform.testutils" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class MessageProcessingBaseCase {
    @Autowired
    protected InformDbOperations dbOps;
    @Autowired
    protected EncounterRepository encounterRepo;
    @Autowired
    protected MrnRepository mrnRepo;
    @Autowired
    protected PatientFactRepository patientFactRepo;

    @Autowired
    protected EmapStarTestUtils emapStarTestUtils;

    protected List<EmapOperationMessage> messageStream = new ArrayList<>();

    /**
     * How far though the message stream processing is.
     */
    protected int nextToProcess = 0;

    /**
     * Process all remaining messages in queue.
     *
     * @throws EmapOperationMessageProcessingException
     */
    @Transactional
    public void processRest() throws EmapOperationMessageProcessingException {
        for (; nextToProcess < messageStream.size(); nextToProcess++) {
            processSingleMessage(messageStream.get(nextToProcess));
        }
    }

    /**
     * Process the next n messages in the list.
     *
     * @param n Number of messages to process.
     * @throws EmapOperationMessageProcessingException.
     * @throws IndexOutOfBoundsException If n is larger than the remaining number of messages.
     */
    @Transactional
    public void processN(int n) throws EmapOperationMessageProcessingException {
        int end = nextToProcess + n;
        for (; nextToProcess < end; nextToProcess++) {
            processSingleMessage(messageStream.get(nextToProcess));
        }
    }

    /**
     * Add a message to the queue.
     *
     * @param msg The message to add.
     */
    public void queueMessage(EmapOperationMessage msg) {
        this.messageStream.add(msg);
    }

    /**
     * Process a single message.
     *
     * @param msg The message to process.
     *
     * @throws EmapOperationMessageProcessingException
     */
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
