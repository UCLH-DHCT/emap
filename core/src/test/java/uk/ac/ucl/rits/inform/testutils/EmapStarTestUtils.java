package uk.ac.ucl.rits.inform.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.EncounterRepository;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Utility methods for making assertions about the contents of Emap-Star.
 * These are used by the (Interchange -> Emap-Star) test cases in
 * uk.ac.ucl.rits.inform.datasinks.emapstar, and by the end to end (HL7 ->->
 * Emap-Star) tests in uk.ac.ucl.rits.inform.tests.
 *
 * @author Jeremy Stein
 */
@Component
@ActiveProfiles("test")
@ComponentScan(basePackages = { "uk.ac.ucl.rits.inform.informdb" })
public class EmapStarTestUtils {

    @Autowired
    protected EncounterRepository encounterRepo;

    public EmapStarTestUtils() {
    }

    /**
     * Check that the encounter got loaded and has the right number of
     * bed visits, and that one of those visits matches the given expected values.
     *
     * @param expectedEncounter the encounter ID to look for
     * @param expectedTotalVisits How many bed/location visits in the encounter in total
     * @param expectedLocation where the patient is expected to be for one of their visits
     * @param expectedDischargeTime for this same visit, the expected discharged time, or null if it's expected to be still open
     * @return the bedVisit found
     */
    @Transactional
    public PatientFact _testVisitExistsWithLocation(String expectedEncounter, int expectedTotalVisits, String expectedLocation, Instant expectedDischargeTime) {
        List<PatientFact> validBedVisits = getLocationVisitsForEncounter(expectedEncounter, expectedTotalVisits);
        List<PatientFact> validBedVisitsAtLocation =
                validBedVisits.stream().filter(f -> f.getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString().equals(expectedLocation)).collect(Collectors.toList());
        assertEquals(1, validBedVisitsAtLocation.size());
        PatientFact bedVisit = validBedVisitsAtLocation.get(0);
        List<PatientProperty> location = bedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid());
        assertEquals(1, location.size(), "There should be exactly one location property for an inpatient bed visit");
        PatientProperty loca = location.get(0);
        assertTrue(loca.isValid());
        assertEquals(expectedLocation, loca.getValueAsString(), "Bedded location not correct");

        List<PatientProperty> dischargeTimes = bedVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, p -> p.isValid());
        if (expectedDischargeTime == null) {
            assertEquals(0, dischargeTimes.size(), "There is an unexpected discharge");
        } else {
            PatientProperty disch = dischargeTimes.get(0);
            assertEquals(expectedDischargeTime, disch.getValueAsDatetime(), "Discharge time does not match");

        }
        return bedVisit;
    }

    /**
     * @param expectedEncounter the encounter, which must exist
     * @param expectedTotalVisits how many visits you expect to exist
     * @return all valid location visit facts (not hospital visits), sorted by arrival time
     */
    @Transactional
    public List<PatientFact> getLocationVisitsForEncounter(String expectedEncounter, int expectedTotalVisits) {
        Encounter enc = encounterRepo.findEncounterByEncounter(expectedEncounter);
        assertNotNull(enc, "encounter did not exist");
        Map<AttributeKeyMap, List<PatientFact>> factsAsMap = enc.getFactsGroupByType();
        assertTrue(!factsAsMap.isEmpty(), "Encounter has no patient facts");
        List<PatientFact> validBedVisits = factsAsMap.getOrDefault(AttributeKeyMap.BED_VISIT, new ArrayList<>()).stream()
                .filter(PatientFact::isValid).collect(Collectors.toList());
        assertEquals(expectedTotalVisits, validBedVisits.size());
        // sort by arrival time
        validBedVisits.sort((v1, v2) ->
                v1.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime().compareTo(
                v2.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime()));
        return validBedVisits;
    }

}
