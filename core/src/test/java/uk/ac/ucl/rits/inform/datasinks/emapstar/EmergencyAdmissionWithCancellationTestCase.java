package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

public class EmergencyAdmissionWithCancellationTestCase extends EmergencyAdmissionTestCase {
    private Instant expectedCorrectedAdmissionTime;
    private String expectedCorrectedLocation;

    public EmergencyAdmissionWithCancellationTestCase() {
    }

    @BeforeEach
    @Override
    public void setup() throws EmapOperationMessageProcessingException {
        // do the original messages, but then cancel the admit and redo it
        super.setup();
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.CANCEL_ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("1234ABCD");
            setVisitNumber("1234567890");
            setPatientClass("E");
            setFullLocationString("ED^null^null");
        }});
        expectedCorrectedAdmissionTime = Instant.now();
        expectedCorrectedLocation = "ED^BADGERS^EURASIAN";
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(expectedCorrectedAdmissionTime);
            setEventOccurredDateTime(expectedCorrectedAdmissionTime);
            setMrn("1234ABCD");
            setVisitNumber("1234567890");
            setPatientClass("E");
            setFullLocationString(expectedCorrectedLocation);
        }});
    }

    /**
     * The expectation for cancelling admission in ED is slightly different, because there is a step before being admitted
     * which is more like being an outpatient. In HL7 this is typically represented with:
     * A04 + A08 + A01 + A11 + A01 + ... + A03
     * Because Star treats A04 and A01 as an admit, and two adjacent admits as an admit + transfer, we
     * need to remember to cancel both bed visits as well as the hospital visit when getting a CANCEL_ADMIT (A11).
     */
    @Override
    @Test
    public void testEdAdmission() {
        Encounter enc = encounterRepo.findEncounterByEncounter("1234567890");
        Map<AttributeKeyMap, List<PatientFact>> factsByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsByType.get(AttributeKeyMap.BED_VISIT);

        assertEquals(2, hospVisits.size());
        Map<Boolean, List<PatientFact>> hospVisitsByValidity = hospVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(1, hospVisitsByValidity.get(false).size());

        List<PatientFact> validHospVisits = hospVisitsByValidity.get(true);
        assertEquals(1, validHospVisits.size());

        assertEquals(3, bedVisits.size());
        Map<Boolean, List<PatientFact>> bedVisitsByValidity = bedVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(2, bedVisitsByValidity.get(false).size());
        List<PatientFact> validBedVisits = bedVisitsByValidity.get(true);
        assertEquals(1, validBedVisits.size());

        // the valid bed visit is the child of the valid hosp visit
        assertEquals(validHospVisits.get(0), validBedVisits.get(0).getParentFact());

        assertEquals(expectedCorrectedAdmissionTime,
                validHospVisits.get(0).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());

        assertEquals(expectedCorrectedAdmissionTime,
                validBedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        assertEquals(expectedCorrectedLocation,
                validBedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());

        // visits are still open
        assertTrue(validHospVisits.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).isEmpty());
        assertTrue(validBedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).isEmpty());
    }
}
