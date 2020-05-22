package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class TestCancelDischarge extends Hl7StreamEndToEndTestCase {
    public TestCancelDischarge() {
        super();
        hl7StreamFileNames.add("DoubleA01WithA13/FirstA01.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/A03.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/A13.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/A03_2.txt");
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
        assertEquals(2, bedDisch.size());

        List<PatientProperty> arrivalTimes = hospitalVisitFacts.get(0).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, arrivalTimes.size());
        List<PatientProperty> dischargeTimes = hospitalVisitFacts.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME);
        assertEquals(2, dischargeTimes.size());  // the invalidated one and the new one
        Map<Boolean, List<PatientProperty>> dischTimes = dischargeTimes.stream().collect(Collectors.partitioningBy(dt -> dt.isValid()));
        // the invalid (cancelled) discharge time property
        assertEquals(Instant.parse("2013-02-11T10:00:00.000Z"), dischTimes.get(false).get(0).getValueAsDatetime());
        // the valid discharge time property has a later value
        assertEquals(Instant.parse("2013-02-11T10:06:00.000Z"), dischTimes.get(true).get(0).getValueAsDatetime());
    }
}
