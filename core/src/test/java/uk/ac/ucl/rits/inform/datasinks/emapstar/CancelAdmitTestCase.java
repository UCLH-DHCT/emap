package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

public class CancelAdmitTestCase extends MessageStreamTestCase {

    private Instant erroneousAdmitTime = Instant.parse("2020-03-01T01:00:00Z");
    private Instant cancellationTime = Instant.parse("2020-03-01T01:02:02Z");
    private Instant correctAdmitTime = Instant.parse("2020-03-01T01:04:04Z");

    public CancelAdmitTestCase() {
    }

    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        String mrn = "1234ABCD";
        String visNum = "1234567890";
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(erroneousAdmitTime);
            setEventOccurredDateTime(erroneousAdmitTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setFullLocationString("ED^BADGERS^WISCONSIN");
        }});
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.CANCEL_ADMIT_PATIENT);
            setEventOccurredDateTime(cancellationTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setFullLocationString("ED^BADGERS^WISCONSIN");
        }});
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(correctAdmitTime);
            setEventOccurredDateTime(correctAdmitTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setFullLocationString("ED^BADGERS^HONEY");
        }});
    }

    /**
     * Do an admit, cancel, admit sequence. In HL7 this would be A01+A11+A01.
     */
    @Test
    @Transactional
    public void testCancelAdmit() {
        Encounter enc = encounterRepo.findEncounterByEncounter("1234567890");
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(2, hospVisits.size());
        assertEquals(2, bedVisits.size());
        assertNull(outpVisits);
        // There should be one invalid bed visit (+hosp visit), and one valid bed visit (+hosp visit)
        Map<Boolean, List<PatientFact>> hospVisitsByValidity = hospVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        Map<Boolean, List<PatientFact>> bedVisitsByValidity = bedVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
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
        
        assertEquals("ED^BADGERS^WISCONSIN", bedVisitsByValidity.get(false).get(0)
                .getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
        assertEquals(erroneousAdmitTime, bedVisitsByValidity.get(false).get(0)
                .getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        assertEquals("ED^BADGERS^HONEY", bedVisitsByValidity.get(true).get(0)
                .getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
        assertEquals(correctAdmitTime, bedVisitsByValidity.get(true).get(0)
                .getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
    }
}
