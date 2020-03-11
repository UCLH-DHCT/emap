package uk.ac.ucl.rits.inform.tests;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Jeremy Stein
 */
public class TestPatientTransferUnknown extends Hl7StreamEndToEndTestCase {
    /**
     * Transferring a patient we have not previously seen should create the patient with the given info.
     * Previous history won't be known but that could potentially be filled in later (from eg. Caboodle).
     */
    public TestPatientTransferUnknown() {
        super();
        hl7StreamFileNames.add("GenericAdt/A02.txt");
    }

    /**
     * Check that the encounter got loaded and has some data associated with it.
     */
    @Test
    @Transactional
    public void testEncounterExists() {
        _testVisitExistsWithLocation("123412341234", 1, "T12S^T12S BY05^BY05-33", null);
    }
}
