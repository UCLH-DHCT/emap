package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;

/**
 * Generatively test different sequences of messages and that the general state
 * after each message looks correct. Possible messages, and state tests are hard
 * coded, while orderings of arbitrary length are generated.
 * <p>
 * In order to keep the database session open for the full duration of each
 * test, tests are run inside of manually created transactions which are all
 * marked as rollback only.
 * <p>
 * Tests are generated such that there should never be a message that is
 * ignored.
 *
 * @author Roma Klapaukh
 */
public class PermutationTestCase extends MessageStreamBaseCase {

    private TransactionTemplate      transactionTemplate;

    @Value("${test.perm.length:2}")
    private int                      maxTestLength;

    /**
     * List of all queueing operations being tested.
     * Operations that don't create demographics must be at the start, with index <= to the noDemoEndIndex
     * Note that queueDischarge must be last.
     */
    private Runnable[]               operations   = {
            this::queueVital,
            this::queueRegister,
            this::queueAdmitTransfer,
            this::queueAdmitClass,
            this::queuePatUpdateClass,
            this::queueTransfer,
            this::queueCancelTransfer,
            this::queueDischarge };

    private final int noDemoEndIndex = 0;

    @SuppressWarnings("unchecked")
    private Hl7Value<PatientClass>[] patientClass = new Hl7Value[] {
            new Hl7Value<>(PatientClass.EMERGENCY),
            new Hl7Value<>(PatientClass.OUTPATIENT), new Hl7Value<>(PatientClass.INPATIENT),
            new Hl7Value<>(PatientClass.DAY_CASE), new Hl7Value<>(PatientClass.SURGICAL_ADMISSION) };
    private int                      currentClass;

    /**
     * Create a new Permutation test case.
     *
     * @param transactionManager Spring transaction manager
     */
    public PermutationTestCase(@Autowired PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Reset the patient state to allow a new message stream to be run.
     */
    private void reset() {
        super.reinitialise();
        currentClass = 0;
        setPatientClass(patientClass[currentClass], super.currentTime);
    }

    /**
     * Queue a registration message
     */
    private void queueRegister() {
        queueRegister(this.getPatientClass());
    }

    /**
     * Queue an admit which will transfer a patient.
     */
    private void queueAdmitTransfer() {
        queueAdmit(true);
    }

    /**
     * Queue and admit which will only change the patient class.
     */
    private void queueAdmitClass() {
        this.currentClass = (this.currentClass + 1) % patientClass.length;
        queueAdmit(false, this.patientClass[this.currentClass]);
    }

    /**
     * Send an Patient update which changes the patient class.
     */
    private void queuePatUpdateClass() {
        this.currentClass = (this.currentClass + 1) % patientClass.length;
        queueUpdatePatientDetails(this.patientClass[this.currentClass]);
    }

    /**
     * Create all the tests.
     *
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    public Stream<DynamicTest> testTransferSequence() {

        List<List<Integer>> initialMessages = new ArrayList<>();
        for (int i = 0; i < operations.length; i++) {
            List<Integer> start = new ArrayList<>();
            start.add(i);
            initialMessages.add(start);
        }
        List<List<Integer>> fullMessages = generatePermutations(initialMessages, 1, this.maxTestLength);

        return fullMessages.stream().map(l -> DynamicTest.dynamicTest("Test " + l.toString(), () -> {
            reset();
            super.mrn = l.toString();
            super.csn = l.toString();
            Exception e = transactionTemplate.execute(status -> {
                status.setRollbackOnly();
                try {
                    runTest(l);
                } catch (EmapOperationMessageProcessingException a) {
                    return a;
                }
                return null;
            });
            if (e != null) {
                throw e;
            }
        }));

    }

    /**
     * Recursive method to generate a permutation of all possible array indexes of
     * the operations array.
     *
     * @param soFar     The permutations created so far. Assumed to be non-empty
     *                  (seed values must be provided)
     * @param step      The current processing step. Usually the maximum list length
     *                  in soFar.
     * @param maxLength How many steps to run total. If step = max(list length in
     *                  soFar) then max length also is the final max list length.
     * @return A list of all possible acceptable orderings of the method calls in
     *         operations.
     */
    private List<List<Integer>> generatePermutations(List<List<Integer>> soFar, int step, int maxLength) {
        if (step == maxLength) {
            return soFar;
        }
        List<List<Integer>> nextRound = new ArrayList<>();

        for (List<Integer> seq : soFar) {
            int last = seq.get(seq.size() - 1);
            if (last == operations.length - 1) {
                // Discharge ends the sequence
                nextRound.add(seq);
            } else {
                // Everything else can have any subsequent message
                for (int i = 0; i < operations.length; i++) {
                    List<Integer> next = new ArrayList<>(seq);
                    next.add(i);
                    nextRound.add(next);
                }
            }
        }

        return generatePermutations(nextRound, step + 1, maxLength);
    }

    /**
     * Run a single stream of indicies as a test. This will involve processing the
     * message stream and checking a subset of properties.
     *
     * @param seq The sequence of messages to send
     * @throws EmapOperationMessageProcessingException If message processing fails.
     */
    private void runTest(List<Integer> seq) throws EmapOperationMessageProcessingException {
        for (int i : seq) {
            operations[i].run();
            processN(1);
            this.checkMrn();
            this.checkAdmission();
            if (i > this.noDemoEndIndex) {
                this.checkDemographics();
            }
            switch (i) {
            case 0:
                testVital();
                break;
            case 1:
                testRegister();
            case 5:
                testLastBedVisit(transferTime.size(), currentLocation().get(), super.getPatientClass().get(),
                        lastTransferTime(), lastTransferTime());
                break;
            case 6:
                testCancelTransfer();
                break;
            }
        }
    }

    /**
     * Ensure that the MRN exists.
     */
    private void checkMrn() {
        assertTrue(super.mrnRepository.getByMrnEquals(super.mrn).isPresent());
    }

    /**
     * Ensure that the presentation time was set, and the visit exists.
     */
    private void testRegister() {
        HospitalVisit visit = this.hospitalVisitRepository.findByEncounter(this.csn).get();

        assertEquals(this.presentationTime.get(), visit.getPresentationTime());
        assertEquals(super.getPatientClass().get().toString(), visit.getPatientClass());
        assertEquals(this.dischargeTime, visit.getDischargeTime());
    }

    /**
     * Test that a vital sign has been correctly added.
     */
    private void testVital() {
        // Not yet implemented
    }

    /**
     * If the last action was a transfer make sure it was processed correctly.
     *
     * @param expectedVisits        How many valid bed moves there should be in
     *                              total
     * @param thisLocation          What the current patient location should be
     * @param thisPatientClass      What the patient class for this location should
     *                              be
     * @param thisLocationStartTime What the admission time for this location should
     *                              be (and the discharge from the last bed).
     * @param eventTime             The time this event was considered to have
     *                              happened.
     */
    private void testLastBedVisit(int expectedVisits, String thisLocation, PatientClass thisPatientClass,
            Instant thisLocationStartTime, Instant eventTime) {

        // Do we have the expected number of Location Visits
        // assertEquals(locationVisits.size(), expectedVisits);

        // If there aren't any to check stop
        if (expectedVisits == 0) {
            return;
        }

//        // Get the preceding visit to check (if this isn't the first)
//        PatientFact precedingVisit = expectedVisits > 1 ? validBedVisits.get(expectedVisits - 2) : null;
//        PatientFact lastVisit = validBedVisits.get(expectedVisits - 1);
//
//        // Check right time / place
//        PatientProperty actualLocation =
//                lastVisit.getPropertyByAttribute(OldAttributeKeyMap.LOCATION, PatientProperty::isValid).get(0);
//        assertEquals(thisLocation, actualLocation.getValueAsString());
//        if (!thisLocationStartTime.equals(Instant.MIN)) {
//            assertEquals(thisLocationStartTime,
//                    lastVisit.getPropertyByAttribute(OldAttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
//        }
//        assertEquals(eventTime, actualLocation.getValidFrom());
//
//        // Check correct invalidation date of patient class (if relevant)
//        Optional<PatientProperty> invPatClass = lastVisit.getPropertyByAttribute(OldAttributeKeyMap.PATIENT_CLASS)
//                .stream().filter(x -> !x.isValid()).findAny();
//        if (invPatClass.isPresent()) {
//            assertEquals(thisLocationStartTime, invPatClass.get().getValidFrom());
//            assertEquals(eventTime, invPatClass.get().getValidUntil());
//        }
//
//        // Check that the last visit closed correctly
//        if (precedingVisit != null) {
//            assertEquals(thisLocationStartTime, precedingVisit.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME)
//                    .get(0).getValueAsDatetime());
//        }

    }

    /**
     * Check that the admission is correct.
     */
    private void checkAdmission() {
        if (this.admissionTime.isUnknown()) {
            return;
        }

        HospitalVisit visit = this.hospitalVisitRepository.findByEncounter(this.csn).get();

        assertEquals(this.admissionTime.get(), visit.getAdmissionTime());
        assertEquals(super.getPatientClass().get().toString(), visit.getPatientClass());
        assertEquals(this.dischargeTime, visit.getDischargeTime());
    }

    /**
     * Check that the patients demographics are correct.
     */
    private void checkDemographics() {
        Mrn mrn = super.mrnRepository.getByMrnEquals(this.mrn).get();
        CoreDemographic demo = super.coreDemographicRepository.getByMrnIdEquals(mrn).get();

        // Check living
        if (this.patientAlive.isUnknown()) {
            assertNull(demo.isAlive());
        } else {
            // Ensure that the death status is correct
            assertEquals(this.patientAlive.get(), demo.isAlive());

            // Ensure death time is correct
            if (this.deathTime.isUnknown()) {
                assertNull(demo.getDatetimeOfDeath(), "Non dead patient had death time");
            } else {
                assertEquals(this.deathTime, demo.getDatetimeOfDeath());
            }
        }

        // Check Name parts
        if (this.fName.isUnknown()) {
            assertNull(demo.getFirstname());
        } else {
            assertEquals(demo.getFirstname(), this.fName.get());
        }
        if (this.mName.isUnknown()) {
            assertNull(demo.getMiddlename());
        } else {
            assertEquals(demo.getMiddlename(), this.mName.get());
        }
        if (this.lName.isUnknown()) {
            assertNull(demo.getLastname());
        } else {
            assertEquals(demo.getLastname(), this.lName.get());
        }


    }

    /**
     * Test that the invalid and unstored orders are present after a cancel transfer.
     */
    public void testCancelTransfer() {

//
//        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
//        Map<OldAttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
//        List<PatientFact> bedVisits = factsGroupByType.get(OldAttributeKeyMap.BED_VISIT);
//
//        // The number of valid bed moves needs to be correct (which may be up to one
//        // less before, or may not)
//        // That's because a cancel transfer can reveal a previously unknown location.
//        assertEquals(this.transferTime.size(), bedVisits.stream().filter(v -> v.isValid()).count());
//
//        // There should exactly one open visit.
//        {
//            List<PatientFact> openBedVisits = bedVisits.stream().filter(PatientFact::isValid)
//                    // It likely has a discharge time. It's just now invalid / unstored.
//                    .filter(v -> v.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME).stream()
//                            .filter(p -> p.isValid()).count() == 0)
//                    .collect(Collectors.toList());
//
//            // After a cancellation there should be exactly 1 open bed visit - the previous
//            // one.
//            assertEquals(1, openBedVisits.size());
//
//            PatientFact bd = openBedVisits.get(0);
//
//            // It should be at the current location
//            assertEquals(this.currentLocation(),
//                    bd.getPropertyByAttribute(OldAttributeKeyMap.LOCATION).get(0).getValueAsString());
//
//            // And at the current start time (unless it is unknown)
//            if (this.lastTransferTime().equals(Instant.MIN)) {
//                // If you don't know, arrival time == null
//                assertNull(bd.getPropertyByAttribute(OldAttributeKeyMap.ARRIVAL_TIME, PatientProperty::isValid).get(0)
//                        .getValueAsDatetime());
//            } else {
//                assertEquals(this.lastTransferTime(),
//                        bd.getPropertyByAttribute(OldAttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
//
//                // It should have a two invalid discharges with the current event time stamp.
//                // But only if it existed beforehand
//                assertEquals(2, bd
//                        .getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME,
//                                p -> !p.isValid()
//                                        && (p.getStoredUntil() != null || this.currentTime.equals(p.getValidUntil())))
//                        .size());
//            }
//        }
//
//        // There should be two variously invalidated visits for this location for this
//        // time.
//        String wrongLocation = this.peekNextLocation();
//
//        // There should be one invalid but stored from the current time bed visit
//        // This will only exist in the 1+ previous bed visits case. But we can only
//        // detect 2+
//        // with the current implementation.
//        if (!this.lastTransferTime().equals(Instant.MIN)) {
//            List<PatientFact> invalidVisit = bedVisits.stream()
//                    .filter(v -> v.getStoredUntil() == null && this.currentTime.equals(v.getValidUntil()))
//                    .collect(Collectors.toList());
//
//            // These should only be one such.
//            assertEquals(1, invalidVisit.size());
//
//            PatientFact iv = invalidVisit.get(0);
//
//            // It should be at the next location
//            assertEquals(wrongLocation,
//                    iv.getPropertyByAttribute(OldAttributeKeyMap.LOCATION).get(0).getValueAsString());
//
//            // All properties should have been invalidated the same way
//            assertTrue(iv.getProperties().stream()
//                    .allMatch(p -> p.getStoredUntil() == null && this.currentTime.equals(p.getValidUntil())));
//
//        }
//
//        // There should be one unstored from the current time but valid bed visit
//        // But only if the last location existed before this
//        if (!this.lastTransferTime().equals(Instant.MIN)) {
//            List<PatientFact> invalidVisit = bedVisits.stream()
//                    .filter(v -> v.getValidUntil() == null && v.getStoredUntil() != null).collect(Collectors.toList());
//
//            // These should only be one such.
//            assertEquals(1, invalidVisit.size());
//
//            PatientFact iv = invalidVisit.get(0);
//
//            // It should be at the next location
//            assertEquals(wrongLocation,
//                    iv.getPropertyByAttribute(OldAttributeKeyMap.LOCATION).get(0).getValueAsString());
//
//            // All properties should have been invalidated the same way
//            assertTrue(
//                    iv.getProperties().stream().allMatch(p -> p.getValidUntil() == null && p.getStoredUntil() != null));
//
//        }

    }
}
