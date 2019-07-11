package uk.ac.ucl.rits.inform.pipeline.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Check that if we get a second A01 for the same encounter where a discharge has not happened in between,
 * that we treat it as a correction to the first one instead of a new admission (which would fail).
 * This is needed because we're not currently receiving A11 (cancel admission) messages, so we have to imply their presence.
 *
 * @author Jeremy Stein
 */
public class DoubleA01Fix extends Hl7StreamTestCase {
    public DoubleA01Fix() {
        super();
        hl7StreamFileNames.add("DoubleA01WithA13/FirstA01.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/SecondA01.txt");
        // For extra robustness check that demographics can be changed
        // (this is needed to trigger at least one bug)
        hl7StreamFileNames.add("DoubleA01WithA13/A08.txt");
    }

    /**
     * Check that the encounter got loaded and has some data associated with it.
     */
    @Test
    @Transactional
    public void testEncounterExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        assertNotNull("encounter did not exist", enc);
        Map<String, PatientFact> factsAsMap = enc.getFactsAsMap();
        assertTrue("Encounter has no patient facts", !factsAsMap.isEmpty());
        PatientFact bedVisit = factsAsMap.get(AttributeKeyMap.BED_VISIT.getShortname());
        List<PatientProperty> location = bedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION);
        assertEquals("There should be exactly one location property for an inpatient bed visit", 1, location.size());
        PatientProperty loca = location.get(0);
        assertTrue(loca.isValid());
        // the location of the second A01 (the correction) should be used
        assertEquals("Bedded location not correct", "T11E^T11E BY02^BY02-17", loca.getValueAsString());
    }
}
