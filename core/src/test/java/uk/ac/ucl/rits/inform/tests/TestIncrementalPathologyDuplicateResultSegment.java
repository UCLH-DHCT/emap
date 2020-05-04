package uk.ac.ucl.rits.inform.tests;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Another tests case for incremental building of pathology results.
 * First message has an order status of  A (some, but not all results available) with 5 results available, but Albumin result duplicated
 * Second message an order status of CM (Order is completed) with 6 results (adding in alanine transaminase, still with Albumin duplicated)
 * Should parse the messages, adding in the new result but not adding in the duplicate albumin fact.
 * @author Stef Piatek
 */
public class TestIncrementalPathologyDuplicateResultSegment extends Hl7StreamEndToEndTestCase {
    public TestIncrementalPathologyDuplicateResultSegment() {
        super();
        hl7StreamFileNames.add("TestIncrementalPathologyDuplicateFactSegment.txt");
    }

    /**
     * Creates a map of results by test code for an order number within an encounter
     * Also checks that encounter exists and only has one order
     * @param encounter   encounter id
     * @param orderNumber order number
     * @return resultByTestCode
     */
    Map<String, List<PatientFact>> mapResultsByTestCode(String encounter, String orderNumber) {
        // ensure that encounter has facts
        List<PatientFact> allFactsForEncounter = encounterRepo.findEncounterByEncounter(encounter).getFacts();
        assertFalse(allFactsForEncounter.isEmpty());

        // only one order
        Map<String, List<PatientFact>> allOrders = allFactsForEncounter.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_ORDER))
                .collect(Collectors.groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER).get(0).getValueAsString()));
        assertEquals(1, allOrders.size());


        // query results by test code
        List<PatientFact> childFacts = allOrders.get(orderNumber).get(0).getChildFacts();
        Map<String, List<PatientFact>> resultsByTestCode = childFacts.stream()
                .filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT))
                .collect(Collectors.groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE).get(0).getValueAsString()));

        return resultsByTestCode;
    }


    /**
     * Given that alanine transaminase is only in the second message, it should have the value of 58
     */
    @Test
    @Transactional
    public void testNewResultIsCorrectValue() {
        Map<String, List<PatientFact>> resultsByTestCode = mapResultsByTestCode("7878787877", "22222222");

        // fact should exist
        List<PatientFact> alanineTransaminaseFacts = resultsByTestCode.get("ALT");
        assertNotNull(alanineTransaminaseFacts);
        assertEquals(1, alanineTransaminaseFacts.size());

        // value should be correct
        PatientFact alanineTransaminaseFact = alanineTransaminaseFacts.get(0);
        double alanineTransaminaseValue = alanineTransaminaseFact.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_NUMERIC_VALUE).get(0).getValueAsReal();
        assertEquals(58, alanineTransaminaseValue);
    }

    /**
     * Given that albumin is given twice within each message, only one fact should exist
     */
    @Test
    @Transactional
    public void testDuplicateResultIsRemoved() {
        Map<String, List<PatientFact>> resultsByTestCode = mapResultsByTestCode("7878787877", "22222222");

        List<PatientFact> albuminFactList = resultsByTestCode.get("ALB");
        assertEquals(1, albuminFactList.size());
    }
}
