package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.Before;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Run the version of this test that includes the admit message.
 * @author Jeremy Stein
 */
public class CancelTransferFull extends CancelTransfer {

    public CancelTransferFull() {
    }

    @Override
    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        setup(false);
    }
}
