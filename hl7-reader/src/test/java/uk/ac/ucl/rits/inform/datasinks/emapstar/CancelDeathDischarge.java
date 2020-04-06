package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * This tests cancelling a discharge where a patient died and
 * then discharging again with a different discharge time.
 * Ensures no discharge-related properties got forgotten.
 * @author Jeremy Stein
 */
public class CancelDeathDischarge extends MessageStreamTestCase {

    String mrn = "1234ABCD";
    String visNum = "1234567890";
    private Instant admitTime = Instant.parse("2020-01-01T01:05:06Z");
    private Instant erroneousDischargeTime = Instant.parse("2020-01-02T19:05:06Z");
    private Instant correctionTime = Instant.parse("2020-01-02T21:15:06Z");
    private Instant correctedDischargeTime = Instant.parse("2020-01-02T20:06:08Z");
    private Instant timeOfDeath = Instant.parse("2020-01-02T17:31:00Z");

    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(admitTime);
            setEventOccurredDateTime(admitTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setFullLocationString("ED^BADGERS^WISCONSIN");
        }});
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setMrn(mrn);
            setVisitNumber(visNum);
            setDischargeDisposition("DIED");
            setDischargeLocation("Patient Died");
            setDischargeDateTime(erroneousDischargeTime);
            setPatientDeathIndicator(true);
            setPatientDeathDateTime(timeOfDeath);
        }});
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.CANCEL_DISCHARGE_PATIENT);
            setMrn(mrn);
            setVisitNumber(visNum);
            setEventOccurredDateTime(correctionTime);
        }});
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setMrn(mrn);
            setVisitNumber(visNum);
            setDischargeDisposition("DIED2");
            setDischargeLocation("Patient Died2");
            setDischargeDateTime(correctedDischargeTime);
            setPatientDeathIndicator(true);
            setPatientDeathDateTime(timeOfDeath);
        }});
    }

    /**
     * Check patient has been discharged as dead.
     */
   @Test
   @Transactional
   public void testCancelDeathDischarge() {
       List<PatientFact> hospVisits = patientFactRepo.findAllByEncounterAndFactType(visNum, AttributeKeyMap.HOSPITAL_VISIT);
       assertEquals(1, hospVisits.size());
       PatientFact hospVisit = hospVisits.get(0);
       List<PatientProperty> dischDisp = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_DISPOSITION, PatientProperty::isValid);
       assertEquals(1, dischDisp.size());
       assertEquals("DIED2", dischDisp.get(0).getValueAsString());

       List<PatientProperty> dischLocation = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_LOCATION, PatientProperty::isValid);
       assertEquals(1, dischLocation.size());
       assertEquals("Patient Died2", dischLocation.get(0).getValueAsString());

       List<PatientProperty> deathIndicator = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR, PatientProperty::isValid);
       assertEquals(1, deathIndicator.size());
       assertEquals(AttributeKeyMap.BOOLEAN_TRUE.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

       List<PatientProperty> dischargeTime = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid);
       assertEquals(1, dischargeTime.size());
       assertEquals(correctedDischargeTime, dischargeTime.get(0).getValueAsDatetime());

       List<PatientProperty> deathTime = hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME, PatientProperty::isValid);
       assertEquals(1, deathTime.size());
       assertEquals(timeOfDeath, deathTime.get(0).getValueAsDatetime());
   }
}
