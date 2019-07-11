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

public class DoubleA01Fix extends Hl7StreamTestCase {

    public DoubleA01Fix() {
        super();
        hl7StreamFileNames.add("DoubleA01.txt");
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
