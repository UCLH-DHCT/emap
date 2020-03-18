/**
 * 
 */
package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;

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

        List<PatientProperty> deathIndicator = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        assertEquals(1, deathIndicator.size());
        assertEquals(AttributeKeyMap.BOOLEAN_TRUE.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

        List<PatientProperty> deathTime = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
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

        List<PatientProperty> deathIndicator = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        assertEquals(1, deathIndicator.size());
        assertEquals(AttributeKeyMap.BOOLEAN_FALSE.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

        // no patient death time if not dead
        List<PatientProperty> deathTime = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
        assertEquals(0, deathTime.size());
    }
}
