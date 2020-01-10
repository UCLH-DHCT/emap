package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * Add vital signs referring to the patient by a retired MRN. This sort of input
 * may be due to a bug in the Caboodle reading code, so may be fixable there.
 *
 * @author Jeremy Stein
 */
public class TestVitalSignsTricky extends VitalSignsTestCase {
    public TestVitalSignsTricky() {
        super();
        // add to older encounter, but refer to the surviving MRN that subsumed its MRN
        messageStream.add(new VitalSigns() {{
            setMrn("ALICE");
            setVisitNumber("dave");
            setVitalSignIdentifier("HEART_RATE");
            //setVitalSignIdentifierCodingSystem("JES");
            setNumericValue(94);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2019-11-14T17:09:58Z"));
        }});

        // add to newer encounter, but refer to the non-surviving MRN
        messageStream.add(new VitalSigns() {{
            setMrn("CAROL");
            setVisitNumber("bob");
            setVitalSignIdentifier("HEART_RATE");
            //setVitalSignIdentifierCodingSystem("JES");
            setNumericValue(95);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2019-11-14T17:09:58Z"));
        }});
    }

    @Test
    @Transactional
    public void testVitalSign() {
        // The vital sign should be found under its original combination of encounter and MRN,
        // even if the wrong (current) MRN was supplied with the vital sign.
        _testHeartRatePresent("CAROL", "dave", 94);
        _testHeartRatePresent("ALICE", "bob", 95);
    }
}
