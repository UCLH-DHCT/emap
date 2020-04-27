package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * A single transfer message for a patient we've never seen before.
 *
 * @author Jeremy Stein
 *
 */
public class ImpliedAdmissionSimpleTestCase extends ImpliedAdmissionTestCase {

    public ImpliedAdmissionSimpleTestCase() {}

    @Override
    @BeforeEach
    public void setup() throws EmapOperationMessageProcessingException {
        performTransfer();
    }
}
