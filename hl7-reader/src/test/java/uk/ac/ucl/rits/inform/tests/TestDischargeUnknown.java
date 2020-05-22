/**
 *
 */
package uk.ac.ucl.rits.inform.tests;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Jeremy Stein
 */
public class TestDischargeUnknown extends Hl7StreamEndToEndTestCase {
    /**
     * Discharging a patient we have not previously seen should create the patient and then discharge them.
     * Transfer record won't be known but that could potentially be filled in later (from eg. Caboodle).
     */
    public TestDischargeUnknown() {
        super();
        hl7StreamFileNames.add("GenericAdt/A03.txt");
    }

    /**
     * Check that the encounter got loaded and has some data associated with it.
     */
    @Test
    @Transactional
    public void testEncounterExists() {
        emapStarTestUtils._testVisitExistsWithLocation("123412341234", 1, "T11E^T11E BY02^BY02-17", Instant.parse("2013-02-11T10:00:00Z"));
    }
}
