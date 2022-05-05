package uk.ac.ucl.rits.inform.datasources.ids.adt;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.datasources.ids.TestHl7MessageStream;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.CancelPendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingEvent;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingType;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests for all pending ADT events
 * @author Stef Piatek
 */
class TestPendingAdt extends TestHl7MessageStream {


    /**
     * Given a pending transfer message
     * When processed
     * The pending event type and pending location should be set
     * @throws Exception shouldn't happen
     */
    @Test
    void testPendingTransferSpecificFields() throws Exception {
        PendingEvent msg = (PendingEvent) processSingleAdtMessage("Adt/pending/A15.txt");

        assertEquals(PendingType.TRANSFER, msg.getPendingEventType());
        assertEquals(InterchangeValue.buildFromHl7("1020100166^SDEC BY02^11 SDEC"), msg.getPendingLocation());
    }

    /**
     * Given a cancel pending transfer message
     * When processed
     * The pending event type, pending location and cancellation datetime should be set
     * @throws Exception shouldn't happen
     */
    @Test
    void testCancelPendingTransferSpecificFields() throws Exception {
        CancelPendingTransfer msg = (CancelPendingTransfer) processSingleAdtMessage("Adt/pending/A26.txt");

        assertEquals(PendingType.TRANSFER, msg.getPendingEventType());
        assertEquals(InterchangeValue.buildFromHl7("1020100166^SDEC BY02^11 SDEC"), msg.getPendingLocation());
        assertEquals(Instant.parse("2022-04-21T23:37:58Z"), msg.getCancelledDateTime());
    }

}
