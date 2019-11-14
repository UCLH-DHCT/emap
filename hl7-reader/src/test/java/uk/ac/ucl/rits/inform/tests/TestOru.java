package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Test processing of an ORU message that refers to an open admission
 * from an A01 message.
 * @author Jeremy Stein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestOru extends Hl7StreamTestCase {
    /**
     * Load in a sequence of pathology message(s) and preceding A01/whatever
     * message(s) to give it somewhere to put the pathology data.
     */
    public TestOru() {
        super();
        hl7StreamFileNames.add("ORU_R01.txt");
    }

    /**
     * Check that the encounter contains some pathology data now.
     */
    @Test
    @Transactional
    public void testPathOrderAndResult() {
        List<PatientFact> orders = patientFactRepo.findAllPathologyOrdersByOrderNumber("12121218");
        assertEquals("should be exactly one order", 1, orders.size());
        PatientFact pathOrder = orders.get(0);
        assertNotNull(pathOrder);

        // check it's the same encounter we find by encounter id
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
        assertEquals(enc, pathOrder.getEncounter());

        // check some attributes of the order
        List<PatientProperty> collectionTimes = pathOrder.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_COLLECTION_TIME);
        assertEquals(1, collectionTimes.size());
        assertEquals(Instant.parse("2013-07-24T15:41:00Z"), collectionTimes.get(0).getValueAsDatetime());
        assertEquals("F",
                pathOrder.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_ORDER_RESULT_STATUS).get(0).getValueAsString());
        assertEquals("H1",
                pathOrder.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_LAB_DEPARTMENT_CODE).get(0).getValueAsString());
        assertEquals("CM",
                pathOrder.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_ORDER_ORDER_STATUS).get(0).getValueAsString());

        List<PatientFact> childFacts = pathOrder.getChildFacts();
        // the order must have a child of type test result that has a battery code value of FBCY
        assertTrue(childFacts.stream().anyMatch(
                pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)
                        && pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE).get(0)
                                .getValueAsString().equals("FBCY")));

        Map<String, List<PatientFact>> resultsByTestCode = childFacts.stream()
                .filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT))
                .collect(Collectors
                        .groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE).get(0).getValueAsString()));

        // within FBCY, check that all the tests exist
        assertEquals(
                new HashSet<>(
                        Arrays.asList("WCC", "RCC", "HGB", "HCTU", "MCVU", "MCHU", "CHCU", "RDWU", "PLT", "MPVU")),
                resultsByTestCode.keySet());

        // check all the properties of one test (WCC)
        List<PatientFact> wccResults = resultsByTestCode.get("WCC");
        assertNotNull(wccResults);
        assertEquals(1, wccResults.size());
        PatientFact wccResult = wccResults.get(0);
        assertTrue(wccResult.getChildFacts().isEmpty());
        assertEquals("WCC",
                wccResult.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE).get(0).getValueAsString());
        assertEquals(new Double(7.28),
                wccResult.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_NUMERIC_VALUE).get(0).getValueAsReal());
        assertEquals("3.0-10.0",
                wccResult.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_REFERENCE_RANGE).get(0).getValueAsString());
        assertEquals("x10^9/L",
                wccResult.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_UNITS).get(0).getValueAsString());
    }
}
