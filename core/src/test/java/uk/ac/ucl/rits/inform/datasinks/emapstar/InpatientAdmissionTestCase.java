package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
        assertEquals(3, hospVisits.size());
        assertEquals(3, bedVisits.size());
        assertNull(outpVisits);

        Map<Pair<Boolean, Boolean>, List<PatientFact>> hospVisitsByValidity = hospVisits.stream().collect(
                Collectors.groupingBy(v -> new ImmutablePair<>(v.getStoredUntil() == null, v.getValidUntil() == null)));
        Map<Pair<Boolean, Boolean>, List<PatientFact>> bedVisitsByValidity = bedVisits.stream().collect(
                Collectors.groupingBy(v -> new ImmutablePair<>(v.getStoredUntil() == null, v.getValidUntil() == null)));
        assertEquals(1, hospVisitsByValidity.get(new ImmutablePair<>(true, true)).size());
        assertEquals(1, hospVisitsByValidity.get(new ImmutablePair<>(false, true)).size());
        assertEquals(1, hospVisitsByValidity.get(new ImmutablePair<>(true, false)).size());
        assertEquals(cancellationTime, hospVisitsByValidity.get(new ImmutablePair<>(true, false)).get(0).getValidUntil());

        assertEquals(1, bedVisitsByValidity.get(new ImmutablePair<>(true, true)).size());
        assertEquals(1, bedVisitsByValidity.get(new ImmutablePair<>(false, true)).size());
        assertEquals(1, bedVisitsByValidity.get(new ImmutablePair<>(true, false)).size());
        assertEquals(cancellationTime, bedVisitsByValidity.get(new ImmutablePair<>(true, false)).get(0).getValidUntil());

        List<PatientProperty> allLocationPropertiesForBedVisit = bedVisits.stream()
                .map(v -> v.getPropertyByAttribute(AttributeKeyMap.LOCATION)).flatMap(List::stream)
                .collect(Collectors.toList());
        emapStarTestUtils._testPropertyValuesOverTime(allLocationPropertiesForBedVisit, wrongLocation, correctLocation,
                erroneousAdmitTime, cancellationTime, correctAdmitTime);

        List<PatientProperty> allArrivalTimePropertiesForBedVisit = bedVisits.stream()
                .map(v -> v.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME)).flatMap(List::stream)
                .collect(Collectors.toList());
        emapStarTestUtils._testPropertyValuesOverTime(allArrivalTimePropertiesForBedVisit, erroneousAdmitTime, correctAdmitTime,
                erroneousAdmitTime, cancellationTime, correctAdmitTime);
    }
}
