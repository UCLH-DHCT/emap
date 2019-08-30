package uk.ac.ucl.rits.inform.tests;

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
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
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
        List<PatientFact> childFactsAsMap = pathOrder.getChildFacts();
        // the order must have a child of type test result that has a battery code value of FBCY
        assertTrue(childFactsAsMap.stream().anyMatch(
                pf -> pf.getFactType().getShortName().equals(AttributeKeyMap.PATHOLOGY_TEST_RESULT.getShortname())
                        && pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE).get(0)
                                .getValueAsString().equals("FBCY")));
    }
}
