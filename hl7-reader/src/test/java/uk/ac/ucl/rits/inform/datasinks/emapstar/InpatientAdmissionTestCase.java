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

public class InpatientAdmissionTestCase extends MessageStreamBaseCase {

    public InpatientAdmissionTestCase() {}

    /**
     * A common ED message sequence is A04, A08, A01. All with patient class E I
     * assume? Is the A01 optional, if eg a patient was turned away at an early
     * stage?
     */
    @Test
    @Transactional
    public void basicAdmit() throws EmapOperationMessageProcessingException {
        this.queueAdmit();
        this.processRest();

        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact onlyHospVisit = hospVisits.get(0);
        assertNull(outpVisits);
        assertEquals(1, bedVisits.size());
        assertIsParentOfChildren(onlyHospVisit, bedVisits);

        assertEquals(this.patientClass,
                onlyHospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());
    }

    @Test
    @Transactional
    public void admitWithCancel() throws EmapOperationMessageProcessingException {
        this.queueAdmit();
        String wrongLocation = currentLocation();
        Instant erroneousAdmitTime = currentTime;
        this.queueCancelAdmit();
        Instant cancellationTime = currentTime;
        this.queueAdmit(true);
        String correctLocation = currentLocation();
        Instant correctAdmitTime = currentTime;
        this.processRest();

        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(2, hospVisits.size());
        assertEquals(2, bedVisits.size());
        assertNull(outpVisits);
        // There should be one invalid bed visit (+hosp visit), and one valid bed visit
        // (+hosp visit)
        Map<Boolean, List<PatientFact>> hospVisitsByValidity =
                hospVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        Map<Boolean, List<PatientFact>> bedVisitsByValidity =
                bedVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(1, hospVisitsByValidity.get(true).size());
        assertEquals(1, hospVisitsByValidity.get(false).size());
        assertEquals(1, bedVisitsByValidity.get(true).size());
        assertEquals(1, bedVisitsByValidity.get(false).size());

        // check the properties are all valid/invalid as appropriate
        List<PatientProperty> propertiesForCancelledBedVisit = bedVisitsByValidity.get(false).get(0).getProperties();
        List<PatientProperty> propertiesForCurrentBedVisit = bedVisitsByValidity.get(true).get(0).getProperties();
        assertTrue(!propertiesForCancelledBedVisit.isEmpty());
        assertTrue(!propertiesForCurrentBedVisit.isEmpty());
        assertTrue(propertiesForCancelledBedVisit.stream().allMatch(p -> !p.isValid()));
        assertTrue(propertiesForCurrentBedVisit.stream().allMatch(p -> p.isValid()));

        // check times and locations are the correct ones
        assertEquals(cancellationTime, hospVisitsByValidity.get(false).get(0).getValidUntil());
        assertEquals(cancellationTime, bedVisitsByValidity.get(false).get(0).getValidUntil());

        assertEquals(wrongLocation, bedVisitsByValidity.get(false).get(0)
                .getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
        assertEquals(erroneousAdmitTime, bedVisitsByValidity.get(false).get(0)
                .getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        assertEquals(correctLocation, bedVisitsByValidity.get(true).get(0)
                .getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
        assertEquals(correctAdmitTime, bedVisitsByValidity.get(true).get(0)
                .getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
    }

}
