package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Test cancel transfer message stream, that can include or
 * exclude the admit message.
 *
 * @author Jeremy Stein
 */
public class CancelTransfer extends MessageStreamBaseCase {

    public String  erroneousTransferLocation, correctTransferLocation, originalLocation;
    public Instant correctTransferTime, erroneousTransferTime, cancellationTime;
    public boolean omitAdmit;

    public CancelTransfer() {}

    @Test
    @Transactional
    public void cancelTransferFull() throws EmapOperationMessageProcessingException {
        omitAdmit = false;
        originalLocation = currentLocation();
        queueAdmit();
        queueTransfer();
        erroneousTransferLocation = currentLocation();
        erroneousTransferTime = currentTime;
        queueCancelTransfer();
        cancellationTime = currentTime;
        // burn a location so the correct location is different to the erroneous one
        nextLocation();
        queueTransfer();
        correctTransferLocation = currentLocation();
        correctTransferTime = currentTime;

        processRest();

    }

    @Test
    @Transactional
    public void cancelTransferMidStream() throws EmapOperationMessageProcessingException {
        omitAdmit = true;
        originalLocation = currentLocation();
        queueTransfer();
        erroneousTransferLocation = currentLocation();
        erroneousTransferTime = currentTime;
        queueCancelTransfer();
        cancellationTime = currentTime;
        // burn a location so the correct location is different to the erroneous one
        nextLocation();
        queueTransfer();
        correctTransferLocation = currentLocation();
        correctTransferTime = currentTime;

        processRest();
    }

    /**
     * Do an admit, transfer, cancel, transfer sequence. In HL7 this would be
     * A01+A02+A12+A02.
     */
    @AfterEach
    @Transactional
    public void testCancelTransfer() {
        Encounter enc = encounterRepo.findEncounterByEncounter("1234567890");
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(1, hospVisits.size());
        assertEquals(4, bedVisits.size());
        assertNull(outpVisits);
        // There should be one invalid bed visit (+hosp visit), and one valid bed visit
        // (+hosp visit)
        Map<Boolean, List<PatientFact>> hospVisitsByValidity =
                hospVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        Map<Boolean, List<PatientFact>> bedVisitsByValidity =
                bedVisits.stream().collect(Collectors.partitioningBy(v -> v.isValid()));
        assertEquals(1, hospVisitsByValidity.get(true).size());
        assertEquals(0, hospVisitsByValidity.get(false).size());
        assertEquals(2, bedVisitsByValidity.get(true).size());
        assertEquals(2, bedVisitsByValidity.get(false).size());

        // check the properties are all valid/invalid as appropriate
        List<PatientProperty> propertiesForCancelledBedVisit = bedVisitsByValidity.get(false).get(0).getProperties();
        assertEquals(erroneousTransferLocation, bedVisitsByValidity.get(false).get(0)
                .getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());

        // don't forget get(1)
        Map<String, List<PatientFact>> validBedVisitsByLocation =
                bedVisitsByValidity.get(true).stream().collect(Collectors.groupingBy(
                        vis -> vis.getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString()));

        // whatever route we took to get here, the current location should be the correct one
        List<PatientFact> correctLocations = validBedVisitsByLocation.get(correctTransferLocation);
        assertEquals(1, correctLocations.size());
        List<PatientProperty> propertiesForCurrentBedVisit = correctLocations.get(0).getProperties();
        assertTrue(!propertiesForCurrentBedVisit.isEmpty());
        assertTrue(propertiesForCurrentBedVisit.stream().allMatch(p -> p.isValid()));

        // in all paths, the erroneous locations should not be valid (one deleted, one invalid)
        Map<String, List<PatientFact>> allBedVisitsByLocation =
                bedVisits.stream().collect(Collectors.groupingBy(
                        vis -> vis.getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString()));
        List<PatientFact> allErroniousBedVisits = allBedVisitsByLocation.get(erroneousTransferLocation);
        assertTrue(allErroniousBedVisits.stream().allMatch(v -> !v.isValid() && v.getProperties().stream().allMatch(p -> !p.isValid())));

        // the invalidation date on the erroneous bed visit matches the cancellation time
        List<PatientFact> allInvalidErroniousBedVisits = allErroniousBedVisits.stream().filter(v -> v.getValidUntil() != null).collect(Collectors.toList());        assertEquals(1, correctLocations.size());
        assertEquals(1, allInvalidErroniousBedVisits.size());
        assertEquals(cancellationTime, allInvalidErroniousBedVisits.get(0).getValidUntil());

        if (!omitAdmit) {
            // If we had >= 2 locations before the cancel transfer the original location gets
            // its discharge property invalidated, and then subsequently re-added when the correct
            // transfer is entered.
            List<PatientFact> originalBedVisits = validBedVisitsByLocation.get(originalLocation);
            assertEquals(1,  originalBedVisits.size());
            PatientFact originalBedVisit = originalBedVisits.get(0);
            List<PatientProperty> allDischargeTimes = originalBedVisit
                    .getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME);

            emapStarTestUtils._testPropertyValuesOverTime(allDischargeTimes,
                    omitAdmit ? null : erroneousTransferTime, correctTransferTime, erroneousTransferTime,
                            cancellationTime, correctTransferTime);
            assertTrue(!propertiesForCancelledBedVisit.isEmpty());
            assertTrue(propertiesForCancelledBedVisit.stream().allMatch(p -> !p.isValid()));
            // all properties except discharge time should be valid
            assertTrue(originalBedVisit.getProperties().stream()
                    .filter(p -> !p.getPropertyType().getShortName().equals(AttributeKeyMap.DISCHARGE_TIME.getShortname()))
                    .allMatch(p -> p.isValid()));
        }

    }
}
