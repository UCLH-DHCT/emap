package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Test different transfers. Note that the Interchange ADT type
 * AdtOperationType.TRANSFER_PATIENT can originate from an A02, A06 or A07 HL7
 * message (or any transfer-like event in a non-HL7 source).
 *
 * @author Jeremy Stein
 *
 */
public class PermutationTestCase extends MessageStreamBaseCase {

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate        transactionTemplate;

    public void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PermutationTestCase() {}

    private Runnable[] operations   = { this::queueAdmitTransfer, this::queueAdmitClass, this::queueVital,
            this::queuePatUpdateClass, this::queueTransfer, this::queueDischarge };
    private String[]   patientClass = { "E", "O", "I", "DAY CASE", "SURG ADMIT" };
    private int        currentClass;

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
        List<List<Integer>> fullMessages = generatePermutations(initialMessages, 1, 2);

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
}
