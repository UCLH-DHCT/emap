package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Test that a straightforward
 * admit -> discharge -> cancel discharge -> discharge
 * sequence works.
 * @author Jeremy Stein
 */
public class TestCancelDischarge extends InterchangeMessageToDbTestCase {
    public TestCancelDischarge() {
        super();
        interchangeMessages.add(messageFactory.getAdtMessage("DoubleA01WithA13/FirstA01.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("DoubleA01WithA13/A03.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("DoubleA01WithA13/A13.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("DoubleA01WithA13/A03_2.yaml", "0000000042"));
    }

    /**
     * Check that the demographics got updated.
     */
    @Test
    @Transactional
    public void testCancelDischargeWorked() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        Map<AttributeKeyMap, List<PatientFact>> factsAsMap = enc.getFactsGroupByType();
        List<PatientFact> generalDemoFacts = factsAsMap.get(AttributeKeyMap.GENERAL_DEMOGRAPHIC);
        assertEquals(1, generalDemoFacts.size());
        List<PatientFact> hospitalVisitFacts = factsAsMap.get(AttributeKeyMap.HOSPITAL_VISIT);
        assertEquals(1, hospitalVisitFacts.size());
        assertTrue(hospitalVisitFacts.get(0).isValid());
        List<PatientFact> bedVisitFacts = factsAsMap.get(AttributeKeyMap.BED_VISIT);
        assertEquals(1, bedVisitFacts.size());
        assertTrue(bedVisitFacts.get(0).isValid());

        List<PatientProperty> bedArrival = bedVisitFacts.get(0).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, bedArrival.size());
        List<PatientProperty> bedDisch = bedVisitFacts.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME);
        assertEquals(3, bedDisch.size());

        List<PatientProperty> arrivalTimes = hospitalVisitFacts.get(0).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, arrivalTimes.size());
        List<PatientProperty> dischargeTimes = hospitalVisitFacts.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME);
        Instant expectedOldValue = Instant.parse("2013-02-11T13:45:00.000Z");
        Instant expectedCurrentValue = Instant.parse("2013-02-11T18:43:00.000Z");
        Instant expectedOldValidUntil = Instant.parse("2013-02-11T13:58:34.000Z");
        emapStarTestUtils._testPropertyValuesOverTime(dischargeTimes, expectedOldValue, expectedCurrentValue,
                expectedOldValue, expectedOldValidUntil, expectedCurrentValue);
    }
}
