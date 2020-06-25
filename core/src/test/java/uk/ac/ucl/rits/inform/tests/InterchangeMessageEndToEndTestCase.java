package uk.ac.ucl.rits.inform.tests;

import ca.uhn.hl7v2.HL7Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.InformDbOperations;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.EncounterRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientFactRepository;
import uk.ac.ucl.rits.inform.datasources.ids.IdsOperations;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.testutils.EmapStarTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test case that first loads in and processes a stream of HL7 messages from one or more text files.
 * @author Jeremy Stein
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {uk.ac.ucl.rits.inform.datasinks.emapstar.App.class})
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@ComponentScan(basePackages = {
        "uk.ac.ucl.rits.inform.datasources.ids",
        "uk.ac.ucl.rits.inform.tests",
        "uk.ac.ucl.rits.inform.testutils",
        "uk.ac.ucl.rits.inform.informdb"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class InterchangeMessageEndToEndTestCase {
    @Autowired
    protected IdsOperations idsOps;
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

    protected InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();

    // Specify which hl7 message containing files you want to be loaded for the test case by adding
    // to this list in order.
    protected List<EmapOperationMessage> interchangeMessages = new ArrayList<>();

    protected int totalMessages;
    protected int processedMessages;

    /**
     * Load in a sequence of HL7 message(s) in preparation for the tests.
     * @throws IOException               if trouble reading the test messages
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if sequence of HL7 data does not make sense
     * @throws MessageIgnoredException   if one or more messages can't be processed
     */
    @BeforeEach
    @Transactional
    public void setup() throws IOException, HL7Exception, Hl7InconsistencyException, EmapOperationMessageProcessingException {
        totalMessages = 0;
        processedMessages = 0;
        if (mrnRepo.count() == 0) {
            for (EmapOperationMessage interchangeMessage : interchangeMessages) {
                totalMessages++;
                // We are bypassing the queue and processing the message immediately, so
                // this is still an end-to-end test (for now).
                // This won't be possible when the HL7 reader is properly split off, then we'll have
                // to split the tests in two as well.
                interchangeMessage.processMessage(dbOps);
                processedMessages++;
            }
        }
    }


    /**
     * All test messages got processed and there was at least one message.
     */
    @Test
    @Transactional
    public void testAllProcessed() {
        assertTrue(!interchangeMessages.isEmpty(), "You must specify some interchange messages!");
        assertTrue(processedMessages > 0, "No messages got processed");
    }

}
