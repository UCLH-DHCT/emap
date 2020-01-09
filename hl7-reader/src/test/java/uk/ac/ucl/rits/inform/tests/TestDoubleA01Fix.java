package uk.ac.ucl.rits.inform.tests;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Check that if we get a second A01 for the same encounter where a discharge has not happened in between,
 * that we treat it as a correction to the first one instead of a new admission (which would fail).
 * This is needed because we're not currently receiving A11 (cancel admission) messages, so we have to imply their presence.
 *
 * @author Jeremy Stein
 */
public class TestDoubleA01Fix extends Hl7StreamTestCase {
    public TestDoubleA01Fix() {
        super();
        hl7StreamFileNames.add("DoubleA01WithA13/FirstA01.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/SecondA01.txt");
        // For extra robustness check that demographics can be changed
        // (this is needed to trigger at least one bug)
        hl7StreamFileNames.add("DoubleA01WithA13/A08.txt");
    }

    /**
     * Check that the encounter got loaded and has some data associated with it.
     */
    @Test
    @Transactional
    public void testEncounterExists() {
        // the location of the second A01 (the correction) should be used
        _testSingleEncounterAndBasicLocation("123412341234", "T11E^T11E BY02^BY02-17", null);
    }
}
