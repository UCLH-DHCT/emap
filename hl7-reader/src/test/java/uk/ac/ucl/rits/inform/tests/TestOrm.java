package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

@Ignore
public class TestOrm extends Hl7StreamEndToEndTestCase {
    /**
     * Load in a sequence of pathology message(s) and preceding A01/whatever
     * message(s) to give it somewhere to put the pathology data.
     */
    public TestOrm() {
        super();
        hl7StreamFileNames.add("PathologyWorkflow1.txt");
    }

    @Test
    @Transactional
    public void testPathologyExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        Map<String, PatientFact> factsAsMap = enc.getFactsAsMap();
        System.out.println(factsAsMap);
        assertTrue(!factsAsMap.isEmpty());
        assertTrue(factsAsMap.containsKey(AttributeKeyMap.PATHOLOGY_ORDER.getShortname()));
    }
}
