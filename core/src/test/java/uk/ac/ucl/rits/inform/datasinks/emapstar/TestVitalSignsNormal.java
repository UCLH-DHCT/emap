package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * Correctly formed vital sign has been received - ie. using the MRN that the
 * encounter belongs under, not the patient's current MRN.
 *
 * @author Jeremy Stein
 */
public class TestVitalSignsNormal extends VitalSignsTestCase {
    public TestVitalSignsNormal() {
        super();
        // add to older encounter, referring to its original but now retired MRN
        messageStream.add(new VitalSigns() {{
            setMrn("CAROL");
            setVisitNumber("dave");
            setVitalSignIdentifier("HEART_RATE");
            //setVitalSignIdentifierCodingSystem("JES");
            setNumericValue(92);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2019-11-14T17:09:58Z"));
        }});
        // add to newer encounter, referring to its original and surviving MRN
        messageStream.add(new VitalSigns() {{
            setMrn("ALICE");
            setVisitNumber("bob");
            setVitalSignIdentifier("HEART_RATE");
            //setVitalSignIdentifierCodingSystem("JES");
            setNumericValue(93);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2019-11-14T17:09:58Z"));
        }});
    }

    @Test
    @Transactional
    public void testVitalSign() {
        _testHeartRatePresent("CAROL", "dave", 92);
        _testHeartRatePresent("ALICE", "bob", 93);
    }
}
