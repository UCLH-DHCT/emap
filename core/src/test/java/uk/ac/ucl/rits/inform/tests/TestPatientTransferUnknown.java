package uk.ac.ucl.rits.inform.tests;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transferring a patient we have not previously seen should create the patient with the given info.
 * Previous history won't be known but that could potentially be filled in later (from eg. Caboodle).
 *
 * @author Jeremy Stein
 */
public class TestPatientTransferUnknown extends InterchangeMessageEndToEndTestCase {
    public TestPatientTransferUnknown() {
        super();
        interchangeMessages.add(messageFactory.getAdtMessage("generic/A02.yaml", "0000000042"));
    }

    /**
     * Check that the encounter got loaded and has some data associated with it.
     */
    @Test
    @Transactional
    public void testVisitExists() {
        emapStarTestUtils._testVisitExistsWithLocation("123412341234", 1, "T12S^T12S BY05^BY05-33", null);
    }
}
