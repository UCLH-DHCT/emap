package uk.ac.ucl.rits.inform.pipeline.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import uk.ac.ucl.rits.inform.pipeline.InformDbOperations;
import uk.ac.ucl.rits.inform.pipeline.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.pipeline.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.pipeline.hl7.HL7Utils;
import uk.ac.ucl.rits.inform.pipeline.informdb.EncounterRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.MrnRepository;

/**
 * A test case that first loads in and processes a stream of HL7 messages from one or more text files.
 * @author Jeremy Stein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
public abstract class Hl7StreamTestCase {
    @Autowired
    protected InformDbOperations dbOps;
    @Autowired
    protected EncounterRepository encounterRepo;
    @Autowired
    protected MrnRepository mrnRepo;

    // Specify which hl7 message containing files you want to be loaded for the test case by adding
    // to this list in order.
    protected List<String> hl7StreamFileNames = new ArrayList<>();

    private int totalMessages;
    private int processedMessages;

    /**
     * Load in a sequence of HL7 message(s) in preparation for the tests.
     * @throws IOException if trouble reading the test messages
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if sequence of HL7 data does not make sense
     * @throws MessageIgnoredException if one or more messages can't be processed
     */
    @Before
    @Transactional
    public void setup() throws IOException, HL7Exception, Hl7InconsistencyException, MessageIgnoredException {
        totalMessages = 0;
        processedMessages = 0;
        if (mrnRepo.count() == 0) {
            for (String resFile : hl7StreamFileNames) {
                Hl7InputStreamMessageIterator hl7Iter = HL7Utils.hl7Iterator(new File(HL7Utils.getPathFromResource(resFile)));
                // populate the database once only
                while (hl7Iter.hasNext()) {
                    totalMessages++;
                    Message msg = hl7Iter.next();
                    processedMessages = dbOps.processHl7Message(msg, totalMessages, null, processedMessages);
                }
            }
        }
    }

    /**
     * All test messages got processed and there was at least one message.
     */
    @Test
    @Transactional
    public void testAllProcessed() {
        assertTrue("You must specify some HL7 containing files", !hl7StreamFileNames.isEmpty());
        assertEquals("not all messages were processed - some were ignored", totalMessages, processedMessages);
        assertTrue("No messages got processed", totalMessages > 0);
    }
}
