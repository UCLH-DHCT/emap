package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * After an implied admission, update the patient details, then transfer.
 * This should not lead to a duplicate fact id, so another update of patient details
 * should not throw and exception
 * @author Stef Piatek
 */
public class ImpliedAdmissionWithVitals7TestCase extends ImpliedAdmissionTestCase {
    public ImpliedAdmissionWithVitals7TestCase() {
    }

    @Override
    @BeforeEach
    public void setup() throws EmapOperationMessageProcessingException {
        addVitals();
        performUpdatePatientDetails();
        performTransfer();
        performUpdatePatientDetails();
    }
}
