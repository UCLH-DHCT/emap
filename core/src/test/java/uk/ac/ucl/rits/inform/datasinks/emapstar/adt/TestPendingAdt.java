package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementRepository;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovement;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.CancelPendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingType;

import java.io.IOException;
import java.io.InvalidClassException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestPendingAdt extends MessageProcessingBase {
    private final Logger logger = LoggerFactory.getLogger(TestPendingAdt.class);
    @Autowired
    private MrnRepository mrnRepository;
    @Autowired
    private CoreDemographicRepository coreDemographicRepository;
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private PlannedMovementRepository plannedMovementRepository;

    // end to end messages
    private PendingTransfer pendingTransfer;
    private CancelPendingTransfer cancelPendingTransfer;

    private static final String VISIT_NUMBER = "123412341234";
    private static final String LOCATION_STRING = "1020100166^SDEC BY02^11 SDEC";
    private static final Instant EVENT_TIME = Instant.parse("2022-04-21T23:22:58Z");
    private static final Instant CANCEL_TIME = Instant.parse("2022-04-21T23:37:58Z");


    private PlannedMovement getPlannedMovementOrThrow(String visitNumber, String location) {
        return plannedMovementRepository
                .findByHospitalVisitIdEncounterAndLocationIdLocationString(visitNumber, location).orElseThrow();
    }

    @BeforeEach
    void setup() throws IOException {
        pendingTransfer = messageFactory.getAdtMessage("pending/A15.yaml");
        cancelPendingTransfer = messageFactory.getAdtMessage("pending/A26.yaml");
    }

    /**
     * Given that no entities exist in the database
     * When a pending transfer message is processed
     * Mrn, core demographics and hospital visit entities should be created
     * @throws Exception shouldn't happen
     */
    @Test
    void testPendingCreatesOtherEntities() throws Exception {
        dbOps.processMessage(pendingTransfer);

        assertEquals(1, mrnRepository.count());
        assertEquals(1, coreDemographicRepository.count());
        assertEquals(1, hospitalVisitRepository.count());
    }

    /**
     * Given no planned movements in the database
     * When a pending transfer is processed
     * Then a new PlannedMovement should be created
     * @throws Exception shouldn't happen
     */
    @Test
    void testPendingCreatesPlannedMovement() throws Exception {
        dbOps.processMessage(pendingTransfer);

        PlannedMovement plannedMovement = getPlannedMovementOrThrow(VISIT_NUMBER, LOCATION_STRING);
        assertEquals(EVENT_TIME, plannedMovement.getEventDatetime());
        assertEquals(PendingType.TRANSFER.toString(), plannedMovement.getEventType());
        assertNull(plannedMovement.getCancelledDatetime());
    }

    /**
     * Given no planned movements in the database
     * When a pending cancel is processed
     * Then a new PlannedMovement should be created with an unknown event datetime
     * @throws Exception shouldn't happen
     */
    @Test
    void testOnlyCancelPendingCreatesPlannedMovement() throws Exception {
        dbOps.processMessage(cancelPendingTransfer);

        PlannedMovement plannedMovement = getPlannedMovementOrThrow(VISIT_NUMBER, LOCATION_STRING);
        assertEquals(CANCEL_TIME, plannedMovement.getCancelledDatetime());
        assertEquals(PendingType.TRANSFER.toString(), plannedMovement.getEventType());
        assertNull(plannedMovement.getEventDatetime());
    }

    /**
     * Given no planned movements in the database
     * When a pending transfer then a pending cancel message is processed
     * Then the planned movement should have the correct event datetime and cancellation time
     * @throws Exception shouldn't happen
     */
    @Test
    void testRequestThenCancelInOrder() throws Exception {
        dbOps.processMessage(pendingTransfer);
        dbOps.processMessage(cancelPendingTransfer);

        PlannedMovement plannedMovement = getPlannedMovementOrThrow(VISIT_NUMBER, LOCATION_STRING);
        assertEquals(EVENT_TIME, plannedMovement.getEventDatetime());
        assertEquals(CANCEL_TIME, plannedMovement.getCancelledDatetime());
    }


    @ParameterizedTest
    @ArgumentsSource(TestMessageStreamProvider.class)
    void testDuplicateRequestInOrder(Iterable<? extends AdtMessage> pendingMessages, Integer expectedCount) throws Exception {
        for (var msg : pendingMessages) {
            if (msg instanceof PendingTransfer) {
                dbOps.processMessage((PendingTransfer) msg);
            } else if (msg instanceof CancelPendingTransfer) {
                dbOps.processMessage((CancelPendingTransfer) msg);
            } else {
                throw new InvalidClassException("Unrecognised class of message");
            }
        }
        List<PlannedMovement> plannedMovements = plannedMovementRepository
                .findAllByHospitalVisitIdEncounterAndLocationIdLocationString(VISIT_NUMBER, LOCATION_STRING);
        assertEquals(expectedCount, plannedMovements.size());
//        for (PlannedMovement pm : plannedMovements) {
//            logger.trace("Testing planned movement has values set correctly");
//            assertNotNull(pm.getCancelledDatetime(), "Cancelled datetime shouldn't be null");
//        }
    }

}


class TestMessageStreamProvider implements ArgumentsProvider {
    private final InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();

    private void changeEventTime(PendingTransfer pendingTransfer, long hourChange) {
        Instant laterTime = pendingTransfer.getEventOccurredDateTime().plus(hourChange, ChronoUnit.HOURS);
        pendingTransfer.setEventOccurredDateTime(laterTime);
    }

    private void changeCancelTime(CancelPendingTransfer cancelPending, long hourChange) {
        Instant laterTime = cancelPending.getCancelledDateTime().plus(hourChange, ChronoUnit.HOURS);
        cancelPending.setEventOccurredDateTime(laterTime);
        cancelPending.setCancelledDateTime(laterTime);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws IOException {
        PendingTransfer pendingTransferEarlier = messageFactory.getAdtMessage("pending/A15.yaml");
        changeEventTime(pendingTransferEarlier, -1);
        PendingTransfer pendingTransfer = messageFactory.getAdtMessage("pending/A15.yaml");
        PendingTransfer pendingTransferLater = messageFactory.getAdtMessage("pending/A15.yaml");
        changeEventTime(pendingTransferLater, 1);

        CancelPendingTransfer cancelPendingEarlier = messageFactory.getAdtMessage("pending/A26.yaml");
        changeCancelTime(cancelPendingEarlier, -1);
        CancelPendingTransfer cancelPending = messageFactory.getAdtMessage("pending/A26.yaml");
        CancelPendingTransfer cancelPendingLater = messageFactory.getAdtMessage("pending/A26.yaml");
        changeCancelTime(cancelPendingLater, 1);

        return Stream.of(
                // simple case of create and cancel, should have single entity
                Arguments.of(List.of(pendingTransfer, cancelPending), 1),
                // request created and cancelled twice, in order
                Arguments.of(List.of(pendingTransfer, cancelPending, pendingTransferLater, cancelPendingLater), 2),
                // request created and cancelled twice, last pending out of order
                Arguments.of(List.of(pendingTransfer, cancelPending, cancelPendingLater, pendingTransferLater), 2),
                // request created and cancelled twice, cancels all received before the pending
                Arguments.of(List.of(cancelPending, cancelPendingLater, pendingTransfer, pendingTransferLater), 2),
                // request created twice, will only output once as we're not sure if it's an update
                Arguments.of(List.of(pendingTransfer, pendingTransferLater), 1),
                // request created twice, cancelled twice, second will only have cancel datetime set
                Arguments.of(List.of(pendingTransfer, pendingTransferLater, cancelPending, cancelPendingLater), 2)
        );
    }

    ;
}
