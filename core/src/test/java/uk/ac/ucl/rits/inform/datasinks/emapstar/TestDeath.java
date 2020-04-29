/**
 *
 */
package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * @author Jeremy Stein
 *
 */
public class TestDeath extends MessageStreamBaseCase {

    public TestDeath() {}

    /**
     * Check patient has been discharged as dead including with cancellations.
     *
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testDeathDischarge() throws EmapOperationMessageProcessingException {
        patientClass = "I";

        queueAdmit();

        dischargeDisposition = "deceased";
        dischargeLocation = "mortuary";
        patientDied = true;
        deathTime = currentTime;

        queueDischarge();

        queueCancelDischarge();

        queueDischarge();

        processRest();

        List<PatientFact> hospVisits =
                patientFactRepo.findAllByEncounterAndFactType(this.csn, AttributeKeyMap.HOSPITAL_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact hospVisit = hospVisits.get(0);
        List<PatientProperty> dischDisp = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_DISPOSITION, PatientProperty::isValid);
        assertEquals(1, dischDisp.size());
        assertEquals(dischargeDisposition, dischDisp.get(0).getValueAsString());

        List<PatientProperty> dischLocation = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_LOCATION, PatientProperty::isValid);
        assertEquals(1, dischLocation.size());
        assertEquals(dischargeLocation, dischLocation.get(0).getValueAsString());

        List<PatientFact> deathFacts =
                patientFactRepo.findAllByEncounterAndFactType(this.csn, AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity =
                deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_TRUE, deathTime);
    }

    /**
     * Check patient has been discharged as alive.
     *
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testLivingDischarge() throws EmapOperationMessageProcessingException {
        queueAdmit();
        dischargeDisposition = "abc";
        dischargeLocation = "home";
        queueDischarge();

        processRest();

        List<PatientFact> hospVisits =
                patientFactRepo.findAllByEncounterAndFactType(this.csn, AttributeKeyMap.HOSPITAL_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact hospVisit = hospVisits.get(0);
        List<PatientProperty> dischDisp = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_DISPOSITION);
        assertEquals(1, dischDisp.size());
        assertEquals(dischargeDisposition, dischDisp.get(0).getValueAsString());

        List<PatientProperty> dischLocation = hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_LOCATION);
        assertEquals(1, dischLocation.size());
        assertEquals(dischargeLocation, dischLocation.get(0).getValueAsString());

        List<PatientFact> deathFacts =
                patientFactRepo.findAllByEncounterAndFactType(this.csn, AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity =
                deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_FALSE, null);
    }

    /**
     * Check patient with death notified by non-discharge message.
     *
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testDeathFactStandard() throws EmapOperationMessageProcessingException {
        queueAdmit();
        deathTime = currentTime;
        patientDied = true;
        queueUpdatePatientDetails();

        processRest();

        List<PatientFact> deathFacts =
                patientFactRepo.findAllByEncounterAndFactType(this.csn, AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity =
                deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_TRUE, deathTime);
    }

    /**
     * Check patient with contradictory death stats notified by non-discharge
     * message, and a redundant transfer that should be ignored.
     *
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testDeathFactContradictory() throws EmapOperationMessageProcessingException {
        patientClass = "I";
        queueAdmit();
        // notify death by a non-discharge message, also simulate one of the weird
        // messages we sometimes get with death = false, but a valid death date

        deathTime = currentTime;
        queueTransfer();

        // Notify death through a redundant transfer (not common - usually an A08)
        patientDied = true;
        queueTransfer(false);

        processRest();

        List<PatientFact> deathFacts =
                patientFactRepo.findAllByEncounterAndFactType(this.csn, AttributeKeyMap.PATIENT_DEATH_FACT);
        Map<Boolean, List<PatientFact>> deathByValidity =
                deathFacts.stream().collect(Collectors.partitioningBy(PatientFact::isValid));
        _checkDeathFact(deathByValidity, AttributeKeyMap.BOOLEAN_FALSE, deathTime);
    }

    /**
     * @param deathByValidity        all death facts for an encounter, partitioned
     *                               by validity
     * @param expectedDeathIndicator AttributeKeyMap.BOOLEAN_FALSE or
     *                               AttributeKeyMap.BOOLEAN_TRUE to indicate the
     *                               expected valid death status
     * @param expectedDeathDateTime  expected valid death datetime
     */
    private void _checkDeathFact(Map<Boolean, List<PatientFact>> deathByValidity,
            AttributeKeyMap expectedDeathIndicator, Instant expectedDeathDateTime) {
        assertEquals(1, deathByValidity.get(true).size());
        PatientFact deathFact = deathByValidity.get(true).get(0);

        List<PatientProperty> deathIndicator =
                deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR);
        assertEquals(1, deathIndicator.size());
        assertEquals(expectedDeathIndicator.getShortname(), deathIndicator.get(0).getValueAsAttribute().getShortName());

        // death time exists but is null
        List<PatientProperty> deathTime = deathFact.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME);
        assertEquals(1, deathTime.size());
        assertEquals(expectedDeathDateTime, deathTime.get(0).getValueAsDatetime());
    }
}
