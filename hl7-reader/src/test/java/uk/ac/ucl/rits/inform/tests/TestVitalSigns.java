/**
 * 
 */
package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * @author Jeremy Stein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestVitalSigns extends MessageStreamTestCase {

    public TestVitalSigns() {
        super();
        VitalSigns vs = new VitalSigns() {{
            setMrn("2222222");
            setVisitNumber("3232323232");
            setVitalSignIdentifier("HEART_RATE");
            //setVitalSignIdentifierCodingSystem("JES");
            setNumericValue(92);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2019-11-14T17:09:58Z"));
        }};
        messageStream.add(vs);
    }

    @Test
    @Transactional
    public void testVitalSign() {
        List<PatientFact> vitalSigns = patientFactRepo.findAllByEncounterAndFactType("3232323232",
                AttributeKeyMap.VITAL_SIGN);
        assertEquals(1, vitalSigns.size());
        PatientFact vit = vitalSigns.get(0);
        Encounter encounter = vit.getEncounter();
        assertEquals("2222222", encounter.getMrns().get(0).getMrn().getMrn());
        assertEquals(new Double(92),
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_NUMERIC_VALUE).get(0).getValueAsReal());
        assertEquals("HEART_RATE",
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_OBSERVATION_IDENTIFIER).get(0).getValueAsString());
        assertEquals("",
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_STRING_VALUE).get(0).getValueAsString());
        assertEquals("/min",
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_UNIT).get(0).getValueAsString());
        assertEquals(Instant.parse("2019-11-14T17:09:58Z"),
                vit.getPropertyByAttribute(AttributeKeyMap.VITAL_SIGNS_OBSERVATION_TIME).get(0).getValueAsDatetime());
    }

}
