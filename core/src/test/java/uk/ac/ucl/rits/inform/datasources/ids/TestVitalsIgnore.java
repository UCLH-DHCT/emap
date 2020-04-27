package uk.ac.ucl.rits.inform.datasources.ids;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

/**
 * Check that a Vital Signs HL7 message is ignored.
 *
 * @author Jeremy Stein
 */
public class TestVitalsIgnore extends TestHl7MessageStream {
    private List<? extends EmapOperationMessage> msgs;

    @BeforeEach
    public void setup() throws Exception {
        msgs = processSingleMessage("GenericAdt/Vitals.txt");
    }

    @Test
    public void testNoMessage() {
        assertTrue(msgs.isEmpty());
    }
}
