package uk.ac.ucl.rits.inform.datasinks.emapstar;

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
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 * @author Roma Klapaukh
 *
 */
public class PermutationTestCase extends MessageStreamBaseCase {

    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate        transactionTemplate;

    @Value("${test.perm.length:2}")
    private int                        maxTestLength;

    private Runnable[]                 operations   = { this::queueAdmitTransfer, this::queueAdmitClass,
            this::queueVital, this::queuePatUpdateClass, this::queueTransfer, this::queueDischarge };
    private String[]                   patientClass = { "E", "O", "I", "DAY CASE", "SURG ADMIT" };
    private int                        currentClass;

    public PermutationTestCase() {}

    public void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

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

    private void queueAdmitTransfer() {
        queueAdmit(true);
    }

    private void queueAdmitClass() {
        this.currentClass = (this.currentClass + 1) % patientClass.length;
        super.patientClass = this.patientClass[this.currentClass];
        queueAdmit();
    }

    private void queuePatUpdateClass() {
        this.currentClass = (this.currentClass + 1) % patientClass.length;
        super.patientClass = this.patientClass[this.currentClass];
        queueUpdatePatientDetails();
    }

    @TestFactory
    public Stream<DynamicTest> testTransferSeqeuence(TestReporter rep) {

        List<List<Integer>> initialMessages = new ArrayList<>();
        for (int i = 0; i < operations.length; i++) {
            List<Integer> start = new ArrayList<>();
            start.add(i);
            initialMessages.add(start);
        }
        List<List<Integer>> fullMessages = generatePermutations(initialMessages, 1, this.maxTestLength);

        return fullMessages.stream().map(l -> makeTest("Test " + l.toString(), () -> {
            reset();
            super.mrn = l.toString();
            super.csn = l.toString();
            setUp();
            Exception e = transactionTemplate.execute(status -> {
                status.setRollbackOnly();
                try {
                    runTest(l, rep);
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

    private DynamicTest makeTest(String name, Executable r) {
        return DynamicTest.dynamicTest(name, r);
    }

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

    public void runTest(List<Integer> seq, TestReporter rep) throws EmapOperationMessageProcessingException {
        for (int i : seq) {
            operations[i].run();
            processN(1);
            this.checkAdmission();
            if (i == 0 || i == 4) {
                testLastBedVisit(transferTime.size(), currentLocation(), super.patientClass, lastTransferTime(),
                        lastTransferTime());
            }
        }
    }

    public void testLastBedVisit(int expectedVisits, String thisLocation, String thisPatientClass,
            Instant thisLocationStartTime, Instant eventTime) {

        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter(this.csn, expectedVisits);

        // Get the last visit to check
        PatientFact precedingVisit = expectedVisits > 1 ? validBedVisits.get(expectedVisits - 2) : null;
        PatientFact lastVisit = validBedVisits.get(expectedVisits - 1);

        // Check right time / place / class
        assertEquals(thisLocation,
                lastVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
        assertEquals(thisLocationStartTime,
                lastVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        // The patient class may have changed, so make sure it is up to date
        PatientProperty patClass = lastVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).stream()
                .filter(x -> x.isValid()).findAny().get();
        assertEquals(thisPatientClass, patClass.getValueAsString());
        assertEquals(eventTime, patClass.getValidFrom());

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

    public void checkAdmission() {
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
}
