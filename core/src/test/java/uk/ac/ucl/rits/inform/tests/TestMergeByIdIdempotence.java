package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.uhn.hl7v2.HL7Exception;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * If we get the merge message twice, the second message should be ignored and
 * the records should stay merged.
 *
 * @author Jeremy Stein
 */
public class TestMergeByIdIdempotence extends TestMergeById {

    public TestMergeByIdIdempotence() {
        // do the merge a second time, should get a MessageIgnoredException
        hl7StreamFileNames.add("GenericAdt/A40.txt");
    }

    @Override
    @BeforeEach
    public void setup()
            throws IOException, HL7Exception, Hl7InconsistencyException, EmapOperationMessageProcessingException {
        assertThrows(MessageIgnoredException.class, () -> super.setup());
    }

    @Override
    @Test
    public void testAllProcessed() {
        assertTrue(!hl7StreamFileNames.isEmpty(), "You must specify some HL7 containing files");
        assertEquals(totalMessages, 1 + processedMessages, "All messages were processed - one should have been ignored");
        assertTrue(totalMessages > 0, "No messages got processed");
    }
}
