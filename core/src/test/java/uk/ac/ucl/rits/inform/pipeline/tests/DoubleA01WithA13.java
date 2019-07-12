package uk.ac.ucl.rits.inform.pipeline.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * The double A01 fix (see DoubleA01Fix.java) can be fooled into not working
 * if there's an A03+A13 (discharge + cancel discharge) in between,
 * if (as is currently the case) we are ignoring A13 messages.
 * This makes it look like the discharge actually happened and the second A01 is a new admission.
 * Reusing a visit number for a new admission is probably the reason this fails in a
 * sticky way (we end up with multiple facts of the same type for an encounter).
 *
 * Jeremy suspects the sequence of events from the user's point of view is:
 * - Create admission (A01)
 * - Realise it's the wrong type of admission (eg. day case vs. inpatient)
 * - Discharge patient in attempt to fix problem (A03)
 * - Decide it would be better to scrap the whole thing and start again (A13+A11, but we don't see the A11)
 * - Admit as the correct admission type (A01)
 *
 * @author Jeremy Stein
 */
public class DoubleA01WithA13 extends Hl7StreamTestCase {
    public DoubleA01WithA13() {
        super();
        hl7StreamFileNames.add("DoubleA01WithA13/FirstA01.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/A03.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/A13.txt");
        hl7StreamFileNames.add("DoubleA01WithA13/SecondA01.txt");
        // bug is not triggered until you then tried to change the demographics again
        hl7StreamFileNames.add("DoubleA01WithA13/A08.txt");
    }

    /**
     * Check that the demographics got updated.
     */
    @Test
    @Transactional
    public void testEncounterExists() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        Map<String, PatientFact> factsAsMap = enc.getFactsAsMap();
        PatientFact generalDemo = factsAsMap.get(AttributeKeyMap.GENERAL_DEMOGRAPHIC.getShortname());
        List<PatientProperty> dob = generalDemo.getPropertyByAttribute(AttributeKeyMap.DOB);
        assertEquals("There should be exactly one dob property", 1, dob.size());
        PatientProperty d = dob.get(0);
        assertTrue(d.isValid());
        // Note the "expected" birthdate being at 23:00 the day before - this is reflecting a bug in the way
        // DOBs are stored - they should be stored as dates only, no times, because that has the
        // potential for timezone bugs as you see here.
        assertEquals("demographics did not get updated", Instant.parse("1989-09-08T23:00:00Z"), d.getValueAsDatetime());
    }
}
