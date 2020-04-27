package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Run the version of this test that excludes the admit message, as if we were starting processing mid-stream.
 * @author Jeremy Stein
 */
public class CancelTransferMidStream extends CancelTransfer {
    public CancelTransferMidStream() {
    }

    @Override
    @BeforeEach
    public void setup() throws EmapOperationMessageProcessingException {
        setup(true);
    }
}
