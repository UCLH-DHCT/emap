package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.OldAttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

@Disabled
public class TestOrm extends InterchangeMessageToDbTestCase {
    /**
     * Load in a sequence of pathology message(s) and preceding A01/whatever
     * message(s) to give it somewhere to put the pathology data.
     */
    public TestOrm() {
        super();
//        interchangeMessages.add("PathologyWorkflow1.txt");
    }

    @Test
    @Transactional
    public void testPathologyExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        Map<String, PatientFact> factsAsMap = enc.getFactsAsMap();
        System.out.println(factsAsMap);
        assertTrue(!factsAsMap.isEmpty());
        assertTrue(factsAsMap.containsKey(OldAttributeKeyMap.PATHOLOGY_ORDER.getShortname()));
    }
}
