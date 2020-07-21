package uk.ac.ucl.rits.inform.tests;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Another tests case for incremental building of pathology results.
 * First message has an order status of  A (some, but not all results available) with 5 results available, but Albumin result duplicated
 * Second message an order status of CM (Order is completed) with 6 results (adding in alanine transaminase, still with Albumin duplicated)
 * Should parse the messages, adding in the new result but not adding in the duplicate albumin fact.
 * @author Stef Piatek
 */
public class TestIncrementalPathologyDuplicateResultSegment extends InterchangeMessageToDbTestCase {
    public TestIncrementalPathologyDuplicateResultSegment() {
        super();
        interchangeMessages.addAll(messageFactory.getPathologyOrders("incremental_duplicate_result_segment.yaml", "0000000042"));
    }

    /**
     * Creates a map of results by test code for an order number.
     * Also checks that it's attached to the right encounter.
     * @param encounter   encounter id
     * @param orderNumber order number
     * @return resultByTestCode
     */
    Map<String, List<PatientFact>> mapResultsByTestCode(String encounter, String orderNumber) {
        List<PatientFact> allOrders = patientFactRepo.findAllPathologyOrdersByOrderNumber(orderNumber);
        assertEquals(1, allOrders.size());
        assertEquals(encounter, allOrders.get(0).getEncounter().getEncounter());
        List<PatientFact> childFacts = allOrders.get(0).getChildFacts();
        Map<String, List<PatientFact>> resultsByTestCode = childFacts.stream()
                .filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT))
                .collect(Collectors.groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE).get(0).getValueAsString()));

        return resultsByTestCode;
    }


    /**
     * Given that alanine transaminase is only in the second message, it should have the value of 58 and be the time of the updated message
     */
    @Test
    @Transactional
    public void testNewResultIsCorrect() {
        Map<String, List<PatientFact>> resultsByTestCode = mapResultsByTestCode("7878787877", "22222222");
        Instant expectedValidFrom = Instant.parse("2020-04-22T05:24:00Z");

        // fact should exist
        List<PatientFact> alanineTransaminaseFacts = resultsByTestCode.get("ALT");
        assertNotNull(alanineTransaminaseFacts);
        assertEquals(1, alanineTransaminaseFacts.size());

        // value should be valid and correct and with the correct valid from
        PatientFact alanineTransaminaseFact = alanineTransaminaseFacts.get(0);
        PatientProperty altProp = alanineTransaminaseFact.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_NUMERIC_VALUE).get(0);
        assertEquals(58, altProp.getValueAsReal());
        assertEquals(expectedValidFrom, altProp.getValidFrom());
        assertTrue(altProp.isValid());

        // date time should be the second datetime message
        Instant resultTime = alanineTransaminaseFact.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_RESULT_TIME).get(0).getValueAsDatetime();
        assertEquals(expectedValidFrom, resultTime);
    }

    /**
     * Given that albumin is given twice within each message, only one fact should exist
     */
    @Test
    @Transactional
    public void testDuplicateResultIsRemoved() {
        Map<String, List<PatientFact>> resultsByTestCode = mapResultsByTestCode("7878787877", "22222222");
        Instant expectedValidFrom = Instant.parse("2020-04-22T03:25:00Z");

        List<PatientFact> albuminFactList = resultsByTestCode.get("ALB");
        assertEquals(1, albuminFactList.size());

        // result was released in first message, and not updated in the second so keep the first message result time
        PatientProperty albuminProperty = albuminFactList.get(0).getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_RESULT_TIME).get(0);
        assertEquals(expectedValidFrom, albuminProperty.getValueAsDatetime());
        assertEquals(expectedValidFrom, albuminProperty.getValidFrom());
        assertTrue(albuminProperty.isValid());
    }
}
