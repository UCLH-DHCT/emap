package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.Assert;
import org.junit.Before;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

/**
 * A transfer message for a patient we've never seen before, but where there's
 * previously been a vital signs message (which creates an encounter but no visit).
 * @author Jeremy Stein
 */
public class ImpliedAdmissionWithVitals7TestCase extends ImpliedAdmissionTestCase {
    public ImpliedAdmissionWithVitals7TestCase() {
    }
    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        addVitals();
        performUpdatePatientDetails();
        performTransfer();
        Assert.assertTrue("Test should fail but doesn't -  see VitalsUpdateTransferTestCase", false);
    }
}
