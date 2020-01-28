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
    /**
     * 
     */
    public TestDeath() {
        super();
        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("CAROL");
            setVisitNumber("dave");
            setPatientClass("I");
        }});

        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setMrn("CAROL");
            setVisitNumber("dave");
            setDischargeDateTime(Instant.now());
            setPatientDeathIndicator(true);
            setPatientDeathDateTime(Instant.now());
        }});
    }

    @Test
    @Transactional
    public void testDeathDischargeStatus() {
        List<PatientFact> hospVisits = patientFactRepo.findAllByEncounterAndFactType("dave", AttributeKeyMap.HOSPITAL_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact hospVisit = hospVisits.get(0);
        List<PatientProperty> dischDisp = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_DISPOSITION);
        List<PatientProperty> deathIndicator = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        List<PatientProperty> deathTime = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
    }
}
