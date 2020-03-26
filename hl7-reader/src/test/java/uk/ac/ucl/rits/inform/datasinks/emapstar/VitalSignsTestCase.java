/**
 * 
 */
package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Set up the patient to add vital signs to. To make it more interesting, give
 * them two MRNs which have been merged.
 *
 * @author Jeremy Stein
 */
public abstract class VitalSignsTestCase extends MessageStreamTestCase {
    public VitalSignsTestCase() {
        super();
        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("ALICE");
            setVisitNumber("bob");
        }});
        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setDischargeDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("ALICE");
            setVisitNumber("bob");
        }});
        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("CAROL");
            setVisitNumber("dave");
        }});
        messageStream.add(new AdtMessage() {{
            setOperationType(AdtOperationType.MERGE_BY_ID);
            setRecordedDateTime(Instant.now());
            setMrn("ALICE");
            setMergedPatientId("CAROL");
        }});
    }

    public void _testHeartRatePresent(String mrnStr, String encounterStr, int expectedHeartRate) {
        List<PatientFact> vitalSigns = patientFactRepo.findAllByEncounterAndFactType(encounterStr,
                AttributeKeyMap.VITAL_SIGN);
        assertEquals(1, vitalSigns.size());
        PatientFact vit = vitalSigns.get(0);
        Encounter encounter = vit.getEncounter();
        assertEquals(mrnStr, encounter.getMrns().get(0).getMrn().getMrn());
        assertEquals(new Double(expectedHeartRate),
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_NUMERIC_VALUE).get(0).getValueAsReal());
        assertEquals("HEART_RATE",
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_OBSERVATION_IDENTIFIER).get(0).getValueAsString());
        assertTrue(vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_STRING_VALUE).isEmpty());
        assertEquals("/min",
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_UNIT).get(0).getValueAsString());
        assertEquals(Instant.parse("2019-11-14T17:09:58Z"),
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_OBSERVATION_TIME).get(0).getValueAsDatetime());
    }
}
