package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.Before;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * A single transfer message for a patient we've never seen before.
 * @author Jeremy Stein
 *
 */
public class ImpliedAdmissionSimpleTestCase extends ImpliedAdmissionTestCase {

    public ImpliedAdmissionSimpleTestCase() {
    }

    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        performTransfer();
    }
}
