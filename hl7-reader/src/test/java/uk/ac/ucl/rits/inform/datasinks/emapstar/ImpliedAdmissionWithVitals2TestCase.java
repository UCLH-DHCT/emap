package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.Before;
import org.junit.Ignore;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * A transfer message for a patient we've never seen before, but where there's
 * previously been a vital signs message (which creates an encounter but no visit).
 *
 * This currently fails because the encounter has been created previously, and
 * the core processor therefore assumes the visit must already exist, and gets
 * upset when it doesn't.
 *
 * @author Jeremy Stein
 */
@Ignore
public class ImpliedAdmissionWithVitals2TestCase extends ImpliedAdmissionTestCase {
    public ImpliedAdmissionWithVitals2TestCase() {
    }

    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        addVitals();
        performTransfer();
    }
}
