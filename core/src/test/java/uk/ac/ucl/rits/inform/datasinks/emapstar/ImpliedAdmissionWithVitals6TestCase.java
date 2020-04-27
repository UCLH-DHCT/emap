package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * A cancel discharge message for a patient we've never seen before, but where
 * there's previously been a vital signs message (which creates an encounter but
 * no visit).
 *
 * @author Jeremy Stein
 */
public class ImpliedAdmissionWithVitals6TestCase extends ImpliedAdmissionTestCase {
    public ImpliedAdmissionWithVitals6TestCase() {}

    @Override
    @BeforeEach
    public void setup() throws EmapOperationMessageProcessingException {
        addVitals();
        performCancelDischarge();
    }
}
