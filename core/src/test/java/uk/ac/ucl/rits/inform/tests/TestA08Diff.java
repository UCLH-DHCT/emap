package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Check that A08 message change the demographics, and that replayed A08 messages
 * don't change anything.
 *
 * @author Jeremy Stein
 */
public class TestA08Diff extends InterchangeMessageEndToEndTestCase {
    public TestA08Diff() {
        super();
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A01.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A08_v1.yaml", "0000000042"));
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A08_v2.yaml", "0000000042"));
    }

    /**
     * Check that the encounter got loaded and demographic changes were recorded.
     */
    @Test
    @Transactional
    public void testDemographicsExist() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        assertNotNull(enc, "encounter did not exist");
        List<PatientFact> facts = enc.getFacts();
        assertTrue(!facts.isEmpty(), "Encounter has no patient facts");
        List<PatientFact> generalDemographicFacts = facts.stream()
                .filter(f -> f.isOfType(AttributeKeyMap.GENERAL_DEMOGRAPHIC))
                .sorted((d1, d2) -> d1.getValidFrom().compareTo(d2.getValidFrom())).collect(Collectors.toList());

        // Get all FAMILY_NAME attributes in all NAME_FACT facts.
        // This will work whether the implementation invalidates property by property or
        // invalidates entire facts.
        List<PatientProperty> allFamilyNames = facts.stream()
                .filter(f -> f.isOfType(AttributeKeyMap.NAME_FACT))
                .map(f -> f.getPropertyByAttribute(AttributeKeyMap.FAMILY_NAME))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Instant origValueTime = Instant.parse("2013-02-11T10:00:52Z");
        Instant changeTime = Instant.parse("2013-03-11T10:00:52Z");
        emapStarTestUtils._testPropertyValuesOverTime(allFamilyNames, "ORANGE", "ORANG", origValueTime, changeTime,
                changeTime);
    }
}
