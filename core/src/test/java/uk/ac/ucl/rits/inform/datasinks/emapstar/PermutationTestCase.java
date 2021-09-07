package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * @author Roma Klapaukh
 */
public class PermutationTestCase extends MessageStreamBaseCase {

    private TransactionTemplate transactionTemplate;

    @Value("${test.perm.length:2}")
    private int maxTestLength;

    /**
     * List of all queueing operations being tested. Operations that don't create
     * demographics must be at the start, with index <= to the noDemoEndIndex Note
     * that queueDischarge must be last.
     */
    private final Runnable[] operations = {
            this::queueFlowsheet,                                                                             // 0
            this::queueRegister,                                                                              // 1
            this::queueAdmitTransfer,                                                                         // 2
            this::queueAdmitClass,                                                                            // 3
            this::queueTransfer,                                                                              // 4
            this::queueCancelTransfer,                                                                        // 5
            this::queueDischarge                                                                              // 6
    };

    private final int noDemoEndIndex = 0;

    @SuppressWarnings("unchecked")
    private final InterchangeValue<PatientClass>[] patientClass = new InterchangeValue[]{new InterchangeValue<>(PatientClass.EMERGENCY),
            new InterchangeValue<>(PatientClass.OUTPATIENT), new InterchangeValue<>(PatientClass.INPATIENT),
            new InterchangeValue<>(PatientClass.DAY_CASE), new InterchangeValue<>(PatientClass.SURGICAL_ADMISSION)};
    private int currentClass;

    /**
     * Create a new Permutation test case.
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
     * Queue a registration message.
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
     * Create all the tests.
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
     * @param soFar     The permutations created so far. Assumed to be non-empty
     *                  (seed values must be provided)
     * @param step      The current processing step. Usually the maximum list length
     *                  in soFar.
     * @param maxLength How many steps to run total. If step = max(list length in
     *                  soFar) then max length also is the final max list length.
     * @return A list of all possible acceptable orderings of the method calls in
     * operations.
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
     * @param seq The sequence of messages to send
     * @throws EmapOperationMessageProcessingException If message processing fails.
     */
    private void runTest(List<Integer> seq) throws EmapOperationMessageProcessingException {
        for (int i : seq) {
            operations[i].run();
            processN(1);
            this.checkMrn();
            this.checkEncounter();
            if (i > this.noDemoEndIndex) {
                this.checkDemographics();
            }
            switch (i) {
                case 0:
                    testVital();
                    break;
                case 1:
                    testRegister();
                    break;
                case 4:
                    testLastBedVisit(transferTime.size(), currentLocation().get(), super.getPatientClass().get(),
                            lastTransferTime(), lastTransferTime());
                    break;
                case 5:
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

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(this.csn).get();
        List<LocationVisit> locationVisits = super.locationVisitRepository.findAllByHospitalVisitId(visit);
        // Do we have the expected number of Location Visits
        assertEquals(expectedVisits, locationVisits.size());

        // If there aren't any to check stop
        if (expectedVisits == 0) {
            return;
        }


        locationVisits.sort((o1, o2) -> o1.getAdmissionTime().isBefore(o2.getAdmissionTime()) ? 1 : -1);

        LocationVisit current = locationVisits.get(0);
        assertEquals(current.getLocationId().getLocationString(), this.currentLocation().get());
        assertEquals(thisLocationStartTime, current.getAdmissionTime());

        // Check the discharge of the previous visit
        if (locationVisits.size() > 1) {
            LocationVisit previous = locationVisits.get(1);
            assertEquals(previous.getDischargeTime(), current.getAdmissionTime());
        }
    }

    /**
     * Check that the encounter information is correct.
     */
    private void checkEncounter() {
        if (this.admissionTime.isUnknown() && this.presentationTime.isUnknown()) {
            return;
        }
        HospitalVisit visit = this.hospitalVisitRepository.findByEncounter(this.csn).get();

        if (!this.presentationTime.isUnknown()) {
            assertEquals(this.presentationTime.get(), visit.getPresentationTime());
        }
        if (!this.admissionTime.isUnknown()) {
            assertEquals(this.admissionTime.get(), visit.getAdmissionTime());
        }
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
            assertNull(demo.getAlive());
        } else {
            // Ensure that the death status is correct
            assertEquals(this.patientAlive.get(), demo.getAlive());

            // Ensure death time is correct
            if (this.deathTime.isUnknown()) {
                assertNull(demo.getDatetimeOfDeath(), "Non dead patient had death time");
            } else {
                assertEquals(this.deathTime, InterchangeValue.buildFromHl7(demo.getDatetimeOfDeath()));
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
     * Test that the invalid and unstored orders are present after a cancel
     * transfer.
     */
    public void testCancelTransfer() {

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(this.csn).get();
        List<LocationVisit> locationVisits = super.locationVisitRepository.findAllByHospitalVisitId(visit);
        // Do we have the expected number of Location Visits
        assertEquals(super.transferTime.size(), locationVisits.size());

        // If there aren't any to check being open
        if (super.transferTime.size() == 0) {
            return;
        }

        locationVisits.sort((o1, o2) -> o1.getAdmissionTime().isBefore(o2.getAdmissionTime()) ? 1 : -1);

        LocationVisit current = locationVisits.get(0);
        assertEquals(current.getLocationId().getLocationString(), this.currentLocation().get());
        assertNull(current.getDischargeTime());

    }
}
