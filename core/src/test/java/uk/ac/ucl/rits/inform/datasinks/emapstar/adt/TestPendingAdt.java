package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementRepository;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovement;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingType;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestPendingAdt extends MessageProcessingBase {
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
    private PendingTransfer cancelPendingTransfer;

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
        cancelPendingTransfer = messageFactory.getAdtMessage("pending/A25.yaml");
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
     * Then
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

}

