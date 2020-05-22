package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import uk.ac.ucl.rits.inform.informdb.Attribute;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Generatively test different sequences of messages and that the general state
 * after each message looks correct. Possible messages, and state tests are hard
 * coded, while orderings of arbitrary length are generated.
 *
 * In order to keep the database session open for the full duration of each
 * test, tests are run inside of manually created transactions which are all
 * marked as rollback only.
 *
 * Tests are generated such that there should never be a message that is
 * ignored.
 *
 * @author Roma Klapaukh
 *
 */
public class PermutationTestCase extends MessageStreamBaseCase {

    private TransactionTemplate transactionTemplate;

    @Value("${test.perm.length:2}")
    private int                 maxTestLength;

    /**
     * List of all queueing operations being tested. Note that queueDischarge must be last.
     */
    private Runnable[]          operations   = { this::queueAdmitTransfer, this::queueAdmitClass, this::queueVital,
            this::queuePatUpdateClass, this::queueTransfer, this::queueDischarge };
    private String[]            patientClass = { "E", "O", "I", "DAY CASE", "SURG ADMIT" };
    private int                 currentClass;

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
        currentClass = 0;
        messageStream.clear();
        nextToProcess = 0;
        super.patientClass = patientClass[currentClass];
        this.vitalTime.clear();
        this.transferTime.clear();
        this.admissionTime = null;
        this.dischargeTime = null;
        this.patientDied = false;
        this.deathTime = null;
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
        super.patientClass = this.patientClass[this.currentClass];
        queueAdmit();
    }

    /**
     * Send an Patient update which changes the patient class.
     */
    private void queuePatUpdateClass() {
        this.currentClass = (this.currentClass + 1) % patientClass.length;
        super.patientClass = this.patientClass[this.currentClass];
        queueUpdatePatientDetails();
    }

    /**
     * Create all the tests
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
            this.checkAdmission();
            this.checkDeath();
            if (i == 0 || i == 4) {
                testLastBedVisit(transferTime.size(), currentLocation(), super.patientClass, lastTransferTime(),
                        lastTransferTime());
            }
        }
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
    private void testLastBedVisit(int expectedVisits, String thisLocation, String thisPatientClass,
            Instant thisLocationStartTime, Instant eventTime) {

        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter(this.csn, expectedVisits);

        // Get the last visit to check
        PatientFact precedingVisit = expectedVisits > 1 ? validBedVisits.get(expectedVisits - 2) : null;
        PatientFact lastVisit = validBedVisits.get(expectedVisits - 1);

        // Check right time / place
        PatientProperty actualLocation = lastVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, PatientProperty::isValid).get(0);
        assertEquals(thisLocation, actualLocation.getValueAsString());
        assertEquals(thisLocationStartTime,
                lastVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        assertEquals(eventTime, actualLocation.getValidFrom());

        // Check correct invalidation date of patient class (if relevant)
        Optional<PatientProperty> invPatClass = lastVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).stream()
                .filter(x -> !x.isValid()).findAny();
        if (invPatClass.isPresent()) {
            assertEquals(thisLocationStartTime, invPatClass.get().getValidFrom());
            assertEquals(eventTime, invPatClass.get().getValidUntil());
        }

        // Check that the last visit closed correctly
        if (precedingVisit != null) {
            assertEquals(thisLocationStartTime,
                    precedingVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).get(0).getValueAsDatetime());
        }

    }

    /**
     * Check that the admission is correct.
     */
    private void checkAdmission() {
        if (this.admissionTime == null) {
            return;
        }
        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        assertNotNull(enc, "encounter did not exist");
        Map<AttributeKeyMap, List<PatientFact>> factsAsMap = enc.getFactsGroupByType();
        assertTrue(!factsAsMap.isEmpty(), "Encounter has no patient facts");
        List<PatientFact> hospVisits = factsAsMap.getOrDefault(AttributeKeyMap.HOSPITAL_VISIT, new ArrayList<>())
                .stream().filter(PatientFact::isValid).collect(Collectors.toList());
        assertEquals(1, hospVisits.size());
        // check hospital visit arrival time
        PatientFact hospVisit = hospVisits.get(0);

        { // Ensure that the arrival time is correct
            List<PatientProperty> _hospArrivalTimes =
                    hospVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME, PatientProperty::isValid);
            assertEquals(1, _hospArrivalTimes.size());
            PatientProperty hospArrivalTime = _hospArrivalTimes.get(0);
            assertEquals(this.admissionTime, hospArrivalTime.getValueAsDatetime());
        }
        { // Ensure that the patient class is correct
            List<PatientProperty> patClasses =
                    hospVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS, PatientProperty::isValid);
            assertEquals(1, patClasses.size());
            PatientProperty patClass = patClasses.get(0);
            assertEquals(super.patientClass, patClass.getValueAsString());
        }
        {
            List<PatientProperty> _hospDischTimes =
                    hospVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid);
            if (this.dischargeTime != null) {
                assertEquals(1, _hospDischTimes.size());
                PatientProperty hospDichargeTime = _hospDischTimes.get(0);
                assertEquals(this.dischargeTime, hospDichargeTime.getValueAsDatetime());
            } else {
                assertTrue(_hospDischTimes.isEmpty(), "Non discharged patient had discharge time");
            }
        }
    }

    /**
     * Check that the patients death status is correct.
     */
    private void checkDeath() {
        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        assertNotNull(enc, "encounter did not exist");
        Map<AttributeKeyMap, List<PatientFact>> factsAsMap = enc.getFactsGroupByType();
        assertTrue(!factsAsMap.isEmpty(), "Encounter has no patient facts");
        List<PatientFact> deaths = factsAsMap.getOrDefault(AttributeKeyMap.PATIENT_DEATH_FACT, new ArrayList<>())
                .stream().filter(PatientFact::isValid).collect(Collectors.toList());
        assertTrue(2 > deaths.size(), "Must only have 1 or 0 death facts");

        if(deaths.isEmpty()) {
            return;
        }

        // Check death date
        PatientFact death = deaths.get(0);

        { // Ensure that the arrival time is correct
            List<PatientProperty> death_inds =
                    death.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_INDICATOR, PatientProperty::isValid);
            assertEquals(1, death_inds.size());
            PatientProperty death_ind = death_inds.get(0);
            Attribute deathInd = death_ind.getValueAsAttribute();
            assertNotNull(deathInd, "Patient death indicator shouldn't be null");
            assertEquals(this.patientDied ?
                            AttributeKeyMap.BOOLEAN_TRUE.getShortname() :
                            AttributeKeyMap.BOOLEAN_FALSE.getShortname(),
                            deathInd.getShortName());
        }
        {
            List<PatientProperty> deathDates =
                    death.getPropertyByAttribute(AttributeKeyMap.PATIENT_DEATH_TIME, PatientProperty::isValid);
            if (this.deathTime != null) {
                assertEquals(1, deathDates.size());
                PatientProperty deathDate = deathDates.get(0);
                assertEquals(this.deathTime, deathDate.getValueAsDatetime());
            } else {
                assertTrue(deathDates.isEmpty(), "Non dead patient had death time");
            }
        }
    }
}
