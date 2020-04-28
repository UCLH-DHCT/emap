/**
 * 
 */
package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * @author Jeremy Stein
 *
 */
public class TestDeath extends MessageStreamTestCase {
    private Instant expectedAdmissionTime = Instant.parse("2020-01-01T11:51:36Z");
    private Instant expectedDeathTime = Instant.parse("2020-01-02T14:05:06Z");
    private Instant expectedDischargeTime = Instant.parse("2020-01-02T16:05:06Z");
    public TestDeath() {
    }

    @Before
    public void setupDave() throws EmapOperationMessageProcessingException {
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(expectedAdmissionTime);
            setEventOccurredDateTime(expectedAdmissionTime);
            setMrn("22222");
            setVisitNumber("dave");
            setPatientClass("I");
        }});

        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setMrn("22222");
            setVisitNumber("dave");
            setDischargeDisposition("deceased");
            setDischargeLocation("mortuary");
            setDischargeDateTime(expectedDischargeTime);
            setEventOccurredDateTime(expectedDischargeTime);
            setPatientDeathIndicator(true);
            setPatientDeathDateTime(expectedDeathTime);
        }});
    }

    @Before
    public void setupAlice() throws EmapOperationMessageProcessingException {
        Instant expectedDoB = Instant.now().minusSeconds(234782378);
        // notify death by a non-discharge message
        processSingleMessage(true, new AdtMessage() {{
            setOperationType(AdtOperationType.UPDATE_PATIENT_INFO);
            setMrn("33333");
            setVisitNumber("alice");
            setAdmissionDateTime(expectedAdmissionTime);
            // A08 does not fill in event occurred field, so don't do it here
            setRecordedDateTime(Instant.now().minusSeconds(10));
            setPatientDeathIndicator(false);
            setPatientDeathDateTime(null);
            setNhsNumber("3456");
            setPatientBirthDate(expectedDoB);
            setPatientClass("I");
            setFullLocationString("T03N^I");
        }});

        // notify death by a non-discharge message
        processSingleMessage(true, new AdtMessage() {{
            setOperationType(AdtOperationType.UPDATE_PATIENT_INFO);
            setMrn("33333");
            setVisitNumber("alice");
            setAdmissionDateTime(expectedAdmissionTime);
            // A08 does not fill in event occurred field, so don't do it here
            setRecordedDateTime(Instant.now().minusSeconds(10));
            setPatientDeathIndicator(false);
            setPatientDeathDateTime(null);
            setNhsNumber("3456");
            setPatientBirthDate(expectedDoB);
            setPatientClass("I");
            setFullLocationString("T03N^I");
        }});

        // notify death by a non-discharge message, also simulate one of the weird
        // messages we sometimes get with death = false, but a valid death date
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.TRANSFER_PATIENT);
            setMrn("33333");
            setVisitNumber("alice");
            setAdmissionDateTime(expectedAdmissionTime);
            setEventOccurredDateTime(Instant.now());
            setPatientDeathIndicator(false);
            setPatientDeathDateTime(null);
            setNhsNumber("3456");
            setPatientBirthDate(expectedDoB);
            setPatientClass("I");
            setFullLocationString("T03N^J");
        }});

        // notify death by a non-discharge message
        processSingleMessage(true, new AdtMessage() {{
            setOperationType(AdtOperationType.UPDATE_PATIENT_INFO);
            setMrn("33333");
            setVisitNumber("alice");
            setAdmissionDateTime(expectedAdmissionTime);
            // A08 does not fill in event occurred field, so don't do it here
            setRecordedDateTime(Instant.now().minusSeconds(10));
            setPatientDeathIndicator(false);
            setPatientDeathDateTime(null);
            setNhsNumber("3456");
            setPatientBirthDate(expectedDoB);
            setPatientClass("I");
            setFullLocationString("T03N^J");
        }});
    }
    
    @Before
    public void setupCarol() throws EmapOperationMessageProcessingException {
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("44444");
            setVisitNumber("carol");
            setPatientClass("I");
            setFullLocationString("T03N^ABC");
        }});

        // notify death by a non-discharge message
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.UPDATE_PATIENT_INFO);
            setMrn("44444");
            setVisitNumber("carol");
            // A08 does not fill in event occurred field, so don't do it here
            setRecordedDateTime(expectedDeathTime);
            setPatientDeathIndicator(true);
            setPatientDeathDateTime(expectedDeathTime);
            setFullLocationString("T03N^ABC");
        }});

    }
    
    @Before
    public void setupBob() throws EmapOperationMessageProcessingException {
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("55555");
            setVisitNumber("bob");
            setPatientClass("I");
            setFullLocationString("T03N^XYZ");
        }});

        // notify death by a non-discharge message, also simulate one of the weird
        // messages we sometimes get with death = false, but a valid death date
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.TRANSFER_PATIENT);
            setMrn("55555");
            setVisitNumber("bob");
            setEventOccurredDateTime(expectedDeathTime);
            setPatientDeathIndicator(false);
            setPatientDeathDateTime(expectedDeathTime);
            setPatientClass("I");
            setFullLocationString("T03N^HIJ");
        }});

        /**
         * Notify death by a redundant transfer message. For now, allow it to be ignored
         * as redundant, as there's potentially quite a lot to check in a transfer
         * message to determine if it really is redundant and I don't know if we want to
         * do that. Main thing is to check that a harder to handle error isn't thrown
         * here.
         */
        processSingleMessage(true, new AdtMessage() {{
            setOperationType(AdtOperationType.TRANSFER_PATIENT);
            setMrn("55555");
            setVisitNumber("bob");
            setEventOccurredDateTime(expectedDeathTime.plusSeconds(35));
            setPatientDeathIndicator(true);
            setPatientDeathDateTime(expectedDeathTime.plusSeconds(35));
            setPatientClass("I");
            setFullLocationString("T03N^HIJ");
        }});
    }

    /**
     * Check patient has been discharged as dead.
     */
    @Test
    @Transactional
    public void testDeathDischargeDave() {
        List<PatientFact> hospVisits = patientFactRepo.findAllByEncounterAndFactType("dave", AttributeKeyMap.HOSPITAL_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact hospVisit = hospVisits.get(0);
        List<PatientProperty> dischDisp = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_DISPOSITION);
        assertEquals(1, dischDisp.size());
        assertEquals("deceased", dischDisp.get(0).getValueAsString());

        List<PatientProperty> dischLocation = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_LOCATION);
        assertEquals(1, dischLocation.size());
        assertEquals("mortuary", dischLocation.get(0).getValueAsString());
    }

    /**
     * Check dead patient has the correct death fact.
     */
    @Test
    @Transactional
    public void testDeathFactDave() {
        List<PatientFact> deathFacts = patientFactRepo.findAllByEncounterAndFactType("dave", AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity = deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_TRUE, expectedDeathTime);
    }

    /**
     * Check patient has been discharged as alive.
     */
    @Test
    @Transactional
    @Ignore
    public void testDeathDischargeAlice() {
        List<PatientFact> hospVisits = patientFactRepo.findAllByEncounterAndFactType("alice", AttributeKeyMap.HOSPITAL_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact hospVisit = hospVisits.get(0);
        List<PatientProperty> dischDisp = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_DISPOSITION);
        assertEquals(1, dischDisp.size());
        assertEquals("abc", dischDisp.get(0).getValueAsString());

        List<PatientProperty> dischLocation = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_LOCATION);
        assertEquals(1, dischLocation.size());
        assertEquals("home", dischLocation.get(0).getValueAsString());
    }

    /**
     * Check alive patient has the correct death fact.
     */
    @Test
    @Transactional
    public void testDeathFactAlice() {
        List<PatientFact> deathFacts = patientFactRepo.findAllByEncounterAndFactType("alice", AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity = deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        System.out.println("JES: death valid " + deathByValidity.get(true).size());
        System.out.println("JES: death invalid " + deathByValidity.get(false).size());
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_FALSE, null);
    }

    /**
     * Check patient with death notified by non-discharge message.
     */
    @Test
    @Transactional
    public void testDeathFactCarol() {
        List<PatientFact> deathFacts = patientFactRepo.findAllByEncounterAndFactType("carol", AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity = deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_TRUE, expectedDeathTime);
    }

    /**
     * Check patient with contradictory death stats notified by non-discharge
     * message, and a redundant transfer that should be ignored.
     */
    @Test
    @Transactional
    public void testDeathFactBob() {
        List<PatientFact> deathFacts = patientFactRepo.findAllByEncounterAndFactType("bob", AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity = deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_FALSE, expectedDeathTime);
    }

    /**
     * @param deathByValidity        all death facts for an encounter, partitioned
     *                               by validity
     * @param expectedDeathIndicator AttributeKeyMap.BOOLEAN_FALSE or
     *                               AttributeKeyMap.BOOLEAN_TRUE to indicate the
     *                               expected valid death status
     * @param expectedDeathDateTime  expected valid death datetime
     */
    private void _checkDeathFact(Map<Boolean, List<PatientFact>> deathByValidity, AttributeKeyMap expectedDeathIndicator, Instant expectedDeathDateTime) {
        assertEquals(1, deathByValidity.get(true).size());
        PatientFact deathFact = deathByValidity.get(true).get(0);

        List<PatientProperty> deathIndicator = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        assertEquals(1, deathIndicator.size());
        assertEquals(expectedDeathIndicator.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

        // death time exists but is null
        List<PatientProperty> deathTime = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
        assertEquals(1, deathTime.size());
        assertEquals(expectedDeathDateTime, deathTime.get(0).getValueAsDatetime());
    }
}
