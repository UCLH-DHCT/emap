package uk.ac.ucl.rits.inform.pipeline.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
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
@ActiveProfiles("test")
public class OruTest extends Hl7StreamTestCase {
    /**
     * Load in a sequence of pathology message(s) and preceding A01/whatever
     * message(s) to give it somewhere to put the pathology data.
     */
    public OruTest() {
        super();
        hl7StreamFileNames.add("ORU_R01.txt");
    }

    /**
     * Check that the encounter contains some pathology data now.
     */
    @Test
    @Transactional
    public void testPathResultExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        Map<String, PatientFact> factsAsMap = enc.getFactsAsMap();
        assertTrue(!factsAsMap.isEmpty());
        PatientFact pathOrder = factsAsMap.get(AttributeKeyMap.PATHOLOGY_ORDER.getShortname());
        assertNotNull(pathOrder);
        Map<String, PatientFact> childFactsAsMap = pathOrder.getChildFactsAsMap();
        assertTrue(childFactsAsMap.containsKey(AttributeKeyMap.PATHOLOGY_TEST_RESULT.getShortname()));
    }
}
