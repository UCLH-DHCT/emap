package uk.ac.ucl.rits.inform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import uk.ac.ucl.rits.inform.hl7.HL7Utils;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.EncounterRepository;
import uk.ac.ucl.rits.inform.informdb.MrnRepository;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

/**
 * Test processing of an ORU message that refers to an open admission
 * from an A01 message.
 * @author Jeremy Stein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
public class OruTest {
    @Autowired
    private InformDbOperations dbOps;
    @Autowired
    private EncounterRepository encounterRepo;
    @Autowired
    private MrnRepository mrnRepo;

    private int totalMessages;
    private int processedMessages;

    /**
     * Load in a sequence of pathology message(s) and preceding A01/whatever
     * message(s) to give it somewhere to put the pathology data.
     * @throws IOException if trouble reading the test messages
     * @throws HL7Exception if HAPI does
     */
    @Before
    @Transactional
    public void setup() throws IOException, HL7Exception {
        Hl7InputStreamMessageIterator hl7Iter = HL7Utils.hl7Iterator(new File(HL7Utils.getPathFromResource("ORU_R01.txt")));
        totalMessages = 0;
        processedMessages = 0;
        // populate the database once only
        if (mrnRepo.count() == 0) {
            while (hl7Iter.hasNext()) {
                totalMessages++;
                Message msg = hl7Iter.next();
                processedMessages = dbOps.processHl7Message(msg, totalMessages, null, processedMessages);
            }
        }
    }

    /**
     * All test messages got processed.
     */
    @Test
    @Transactional
    public void testAllProcessed() {
        assertEquals(totalMessages, processedMessages);
    }

    /**
     */
    @Test
    @Transactional
    public void testPathResultExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        Map<String, PatientFact> factsAsMap = enc.getFactsAsMap();
        assertTrue(!factsAsMap.isEmpty());
        assertTrue(factsAsMap.containsKey(AttributeKeyMap.PATHOLOGY_TEST_RESULT.getShortname()));
    }
}
