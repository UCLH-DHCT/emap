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
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Base class for testing cancel transfer message stream, that can include or exclude the admit message.
 * @author Jeremy Stein
 */
public abstract class CancelTransfer extends MessageStreamTestCase {
    String mrn = "1234ABCD";
    String visNum = "1234567890";
    private Instant admitTime = Instant.parse("2020-03-01T00:30:00Z");
    private Instant erroneousTransferTime = Instant.parse("2020-03-01T01:00:00Z");
    private Instant cancellationTime = Instant.parse("2020-03-01T01:02:02Z");
    private Instant correctTransferTime = Instant.parse("2020-03-01T01:04:04Z");
    private String originalLocation = "ED^BADGERS^WISCONSIN";
    private String erroneousTransferLocation = "ED^BADGERS^HONEY";
    private String correctTransferLocation = "ED^BADGERS^EURASIAN";

    private boolean omitAdmit;

    public CancelTransfer() {
    }

    /**
     * Parameterised test.
     * @param omitAdmit iff true, simulate coming in mid-stream by omitting the first admit message
     */
    public void setup(boolean omitAdmit) throws EmapOperationMessageProcessingException {
        this.omitAdmit = omitAdmit;

        if (!omitAdmit) {
            processSingleMessage(new AdtMessage() {{
                setOperationType(AdtOperationType.ADMIT_PATIENT);
                setAdmissionDateTime(admitTime);
                setEventOccurredDateTime(admitTime);
                setMrn(mrn);
                setVisitNumber(visNum);
                setPatientClass("I");
                setPatientFullName("Fred Bloggs");
                setFullLocationString(originalLocation);
            }});
        }

        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.TRANSFER_PATIENT);
            setAdmissionDateTime(admitTime);
            setEventOccurredDateTime(erroneousTransferTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setFullLocationString(erroneousTransferLocation);
        }});

        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.CANCEL_TRANSFER_PATIENT);
            setEventOccurredDateTime(erroneousTransferTime);
            setRecordedDateTime(cancellationTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setFullLocationString(originalLocation);
            // previous location would be set to erroneousTransferLocation if we had that field in Interchange
        }});

        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.TRANSFER_PATIENT);
            setAdmissionDateTime(admitTime);
            setEventOccurredDateTime(correctTransferTime);
            setMrn(mrn);
            setVisitNumber(visNum);
            setPatientClass("I");
            setFullLocationString(correctTransferLocation);
        }});
    }

    /**
     * Do an admit, transfer, cancel, transfer sequence. In HL7 this would be A01+A02+A12+A02.
     */
    @Test
    @Transactional
    public void testCancelTransfer() {
        Encounter enc = encounterRepo.findEncounterByEncounter("1234567890");
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(1, hospVisits.size());
        assertEquals(3, bedVisits.size());
        assertNull(outpVisits);
        // There should be one invalid bed visit (+hosp visit), and one valid bed visit (+hosp visit)
        Map<Boolean, List<PatientFact>> hospVisitsByValidity = hospVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        Map<Boolean, List<PatientFact>> bedVisitsByValidity = bedVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(1, hospVisitsByValidity.get(true).size());
        assertEquals(0, hospVisitsByValidity.get(false).size());
        assertEquals(2, bedVisitsByValidity.get(true).size());
        assertEquals(1, bedVisitsByValidity.get(false).size());

        // check the properties are all valid/invalid as appropriate
        List<PatientProperty> propertiesForCancelledBedVisit = bedVisitsByValidity.get(false).get(0).getProperties();
        assertEquals(erroneousTransferLocation, bedVisitsByValidity.get(false).get(0)
                .getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());

        // don't forget get(1)
        Map<String, List<PatientFact>> validBedVisitsByLocation = bedVisitsByValidity.get(true).stream().collect(Collectors
                .groupingBy(vis -> vis.getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString()));
        List<PatientProperty> propertiesForCurrentBedVisit = validBedVisitsByLocation.get(correctTransferLocation).get(0).getProperties();
        PatientFact originalBedVisit = validBedVisitsByLocation.get(originalLocation).get(0);
        Map<Boolean, List<PatientProperty>> allDischargeTimesByValidity = originalBedVisit
                .getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).stream()
                .collect(Collectors.partitioningBy(p -> p.isValid()));

        // one valid. If admit message was sent, then one invalid should exist too
        assertEquals(1, allDischargeTimesByValidity.get(true).size());
        assertEquals(correctTransferTime, allDischargeTimesByValidity.get(true).get(0).getValueAsDatetime());
        if (omitAdmit) {
            assertEquals(0, allDischargeTimesByValidity.get(false).size());
        } else {
            assertEquals(1, allDischargeTimesByValidity.get(false).size());
            assertEquals(erroneousTransferTime, allDischargeTimesByValidity.get(false).get(0).getValueAsDatetime());
        }

        assertTrue(!propertiesForCancelledBedVisit.isEmpty());
        assertTrue(!propertiesForCurrentBedVisit.isEmpty());
        assertTrue(propertiesForCancelledBedVisit.stream().allMatch(p -> !p.isValid()));
        assertTrue(propertiesForCurrentBedVisit.stream().allMatch(p -> p.isValid()));
        // all properties except discharge time should be valid
        assertTrue(originalBedVisit.getProperties().stream()
                .filter(p -> !p.getPropertyType().getShortName().equals(AttributeKeyMap.DISCHARGE_TIME.getShortname()))
                .allMatch(p -> p.isValid()));

        // check times and locations are the correct ones
        assertEquals(cancellationTime, bedVisitsByValidity.get(false).get(0).getValidUntil());
    }
}
