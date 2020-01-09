package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 *
 * @author Jeremy Stein
 */
public class TestPatientUpdateUnknown extends Hl7StreamTestCase {
    /**
     * Updating info for a patient we have not previously seen should NOT create a patient.
     */
    public TestPatientUpdateUnknown() {
        super();
        hl7StreamFileNames.add("GenericAdt/A08_v1.txt");
    }

    @Override
    public void setup()
            throws IOException, HL7Exception, Hl7InconsistencyException, EmapOperationMessageProcessingException {
        try {
            super.setup();
            fail("Expected exception MessageIgnoredException");
        } catch (MessageIgnoredException th) {
            // good
        }
    }

    /**
     * Check that no messages got processed.
     */
    @Override
    public void testAllProcessed() {
        assertEquals("expecting no messages to be processed", 0, processedMessages);
    }

    /**
     * Check that the encounter did not get get loaded.
     */
    @Test
    @Transactional
    public void testEncounterNotExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        assertNull("encounter exists but should not", enc);
    }
}
