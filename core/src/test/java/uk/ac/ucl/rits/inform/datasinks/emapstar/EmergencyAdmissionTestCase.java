package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

public class EmergencyAdmissionTestCase extends MessageStreamBaseCase {

    public EmergencyAdmissionTestCase() {}

    /**
     * A common ED message sequence is A04, A08, A01. All with patient class E I
     * assume? Is the A01 optional, if eg a patient was turned away at an early
     * stage?
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testEdAdmission() throws EmapOperationMessageProcessingException {
        patientClass = "E";
        queueAdmit();
        queueUpdatePatientDetails();
        queueAdmit(true);

        processRest();

        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertTrue(bedVisits.stream().allMatch(v -> v.isValid()));
        assertEquals(1, hospVisits.size());
        assertEquals(2, bedVisits.size());
        assertNull(outpVisits);
        PatientFact onlyHospVisit = hospVisits.get(0);
        assertIsParentOfChildren(onlyHospVisit, bedVisits);

        // patient class should go in the hosp visit - we think it doesn't
        // routinely change during a visit,
        // except when explicitly signalled, eg. and O->I transfer.
        assertEquals("E",
                hospVisits.get(0).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());
    }

    /**
     * The expectation for cancelling admission in ED is slightly different, because
     * there is a step before being admitted which is more like being an outpatient.
     * In HL7 this is typically represented with: A04 + A08 + A01 + A11 + A01 + ...
     * + A03 Because Star treats A04 and A01 as an admit, and two adjacent admits as
     * an admit + transfer, we need to remember to cancel both bed visits as well as
     * the hospital visit when getting a CANCEL_ADMIT (A11).
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testEdAdmissionWithCancellation() throws EmapOperationMessageProcessingException {
        patientClass = "E";
        queueAdmit();
        queueUpdatePatientDetails();
        queueAdmit(true);

        queueCancelAdmit();
        queueAdmit(true);
        processRest();

        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        Map<AttributeKeyMap, List<PatientFact>> factsByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsByType.get(AttributeKeyMap.BED_VISIT);

        assertEquals(3, hospVisits.size());
        Map<Boolean, List<PatientFact>> hospVisitsByValidity =
                hospVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(2, hospVisitsByValidity.get(false).size());

        List<PatientFact> validHospVisits = hospVisitsByValidity.get(true);
        assertEquals(1, validHospVisits.size());

        assertEquals(5, bedVisits.size());
        Map<Boolean, List<PatientFact>> bedVisitsByValidity =
                bedVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(4, bedVisitsByValidity.get(false).size());
        List<PatientFact> validBedVisits = bedVisitsByValidity.get(true);
        assertEquals(1, validBedVisits.size());

        // the valid bed visit is the child of the valid hosp visit
        assertEquals(validHospVisits.get(0), validBedVisits.get(0).getParentFact());

        assertEquals(this.lastTransferTime(), validHospVisits.get(0)
                .getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());

        assertEquals(this.lastTransferTime(),
                validBedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        assertEquals(this.currentLocation(),
                validBedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());

        // visits are still open
        assertTrue(validHospVisits.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).isEmpty());
        assertTrue(validBedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).isEmpty());
    }


    /**
     * An ED patient transfers to a inpatient ward (A06). We expect hospital visit fact
     * to change patient class form 'E' to 'I'
     * @throws EmapOperationMessageProcessingException
     */
    @Test
    @Transactional
    public void testEdAdmissionAsInpatient() throws EmapOperationMessageProcessingException {
        patientClass = "E";
        queueAdmit();
        Instant emergencyStartTime = this.currentTime;
        queueUpdatePatientDetails();
        queueAdmit(true);

        // an 'A06' message ie change patient to inpatient and tranfer
        patientClass = "I";
        queueAdmit(true);
        Instant toInpatientTime = this.currentTime;

        processRest();

        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        Map<AttributeKeyMap, List<PatientFact>> factsByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsByType.get(AttributeKeyMap.BED_VISIT);
        assertTrue(bedVisits.stream().allMatch(v -> v.isValid()));
        assertEquals(1, hospVisits.size());
        assertEquals(3, bedVisits.size());

        PatientFact onlyHospVisit = hospVisits.get(0);
        assertIsParentOfChildren(onlyHospVisit, bedVisits);

        List<PatientProperty> allPatientClasses = onlyHospVisit
                .getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS);

        emapStarTestUtils._testPropertyValuesOverTime(allPatientClasses, "E", "I", emergencyStartTime, toInpatientTime, toInpatientTime);
    }
}
