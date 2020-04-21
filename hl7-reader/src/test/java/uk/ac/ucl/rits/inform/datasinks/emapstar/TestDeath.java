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

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * @author Jeremy Stein
 *
 */
public class TestDeath extends MessageStreamTestCase {
    private Instant expectedDeathTime = Instant.parse("2020-01-02T14:05:06Z");
    private Instant expectedDischargeTime = Instant.parse("2020-01-02T16:05:06Z");
    public TestDeath() {
        super();
        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("22222");
            setVisitNumber("dave");
            setPatientClass("I");
        }});

        messageStream.add(new AdtMessage() {{
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

        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("33333");
            setVisitNumber("alice");
            setPatientClass("I");
        }});

        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setMrn("33333");
            setVisitNumber("alice");
            setDischargeDisposition("abc");
            setDischargeLocation("home");
            setDischargeDateTime(expectedDischargeTime);
            setEventOccurredDateTime(expectedDischargeTime);
            setPatientDeathIndicator(false);
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
        assertEquals(1, deathByValidity.get(true).size());
        PatientFact deathFact = deathByValidity.get(true).get(0);

        List<PatientProperty> deathIndicator = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        assertEquals(1, deathIndicator.size());
        assertEquals(AttributeKeyMap.BOOLEAN_TRUE.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

        List<PatientProperty> deathTime = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
        assertEquals(1, deathTime.size());
        assertEquals(expectedDeathTime, deathTime.get(0).getValueAsDatetime());
    }

    /**
     * Check patient has been discharged as alive.
     */
    @Test
    @Transactional
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
        assertEquals(1, deathByValidity.get(true).size());
        PatientFact deathFact = deathByValidity.get(true).get(0);

        List<PatientProperty> deathIndicator = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        assertEquals(1, deathIndicator.size());
        assertEquals(AttributeKeyMap.BOOLEAN_FALSE.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

        // death time exists but is null
        List<PatientProperty> deathTime = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
        assertEquals(1, deathTime.size());
        assertNull(deathTime.get(0).getValueAsDatetime());
    }
}
