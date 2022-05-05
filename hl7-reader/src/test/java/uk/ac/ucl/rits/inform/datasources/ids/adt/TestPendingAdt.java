package uk.ac.ucl.rits.inform.datasources.ids.adt;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.datasources.ids.TestHl7MessageStream;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingType;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests for all pending ADT events
 * @author Stef Piatek
 */
class TestPendingAdt extends TestHl7MessageStream {
    private PendingTransfer pendingTransfer;

    @BeforeEach
    void setup() throws Exception {
        pendingTransfer = (PendingTransfer) processSingleAdtMessage("Adt/pending/A15.txt");
    }

    @Test
    void testPendingTransferSpecificFields() {
        assertEquals(PendingType.TRANSFER, pendingTransfer.getPendingEventType());
        assertEquals(InterchangeValue.buildFromHl7("1020100166^SDEC BY02^11 SDEC"), pendingTransfer.getPendingLocation());
    }

}
