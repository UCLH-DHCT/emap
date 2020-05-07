package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

/**
 *
 * @author Jeremy Stein
 */
public class TestPatientUpdateUnknown extends Hl7StreamEndToEndTestCase {
    /**
     * Updating info for a patient we have not previously seen should create the encounter but no visits.
     */
    public TestPatientUpdateUnknown() {
        super();
        hl7StreamFileNames.add("GenericAdt/A08_v1.txt");
    }

    /**
     * Check that the encounter got created and the right facts do/don't exist.
     */
    @Test
    @Transactional
    public void testEncounterAndFacts() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        assertNotNull(enc, "encounter does not exist");
        Map<AttributeKeyMap, List<PatientFact>> factsByType = enc.getFactsGroupByType();
        assertTrue(factsByType.containsKey(AttributeKeyMap.NAME_FACT));
        assertTrue(factsByType.containsKey(AttributeKeyMap.GENERAL_DEMOGRAPHIC));
        assertTrue(factsByType.containsKey(AttributeKeyMap.PATIENT_DEATH_FACT));
        assertFalse(factsByType.containsKey(AttributeKeyMap.HOSPITAL_VISIT));
        assertFalse(factsByType.containsKey(AttributeKeyMap.BED_VISIT));
    }
}
