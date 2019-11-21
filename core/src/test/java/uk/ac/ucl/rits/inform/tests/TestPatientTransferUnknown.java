package uk.ac.ucl.rits.inform.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Jeremy Stein
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestPatientTransferUnknown extends Hl7StreamTestCase {
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
        _testSingleEncounterAndBasicLocation("123412341234", "T12S^T12S BY05^BY05-33", null);
    }
}
