package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * A transfer message for a patient we've never seen before, followed by a vital
 * signs message.
 *
 * @author Jeremy Stein
 */
public class ImpliedAdmissionWithVitals1TestCase extends ImpliedAdmissionTestCase {

    public ImpliedAdmissionWithVitals1TestCase() {}

    @Override
    @BeforeEach
    public void setup() throws EmapOperationMessageProcessingException {
        performTransfer();
        addVitals();
    }
}
