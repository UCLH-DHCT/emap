package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * A discharge message for a patient we've never seen before, but where there's
 * previously been a vital signs message (which creates an encounter but no visit).
 *
 * @author Jeremy Stein
 */
public class ImpliedAdmissionWithVitals3TestCase extends ImpliedAdmissionTestCase {
    public ImpliedAdmissionWithVitals3TestCase() {
    }

    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        addVitals();
        performDischarge();
    }
}
