package uk.ac.ucl.rits.inform.tests;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Another tests case for incremental biulding of pathology results.
 * First message has an order status of  A (some, but not all results available) with 5 results available, but Albumin result duplicated
 * Second message an order status of CM (Order is completed) with 6 results (adding in alanine transaminase, still with Albumin dulpicated)
 * Should parse the messages, adding in the new result but not adding in the duplicate albumin fact.
 *
 * @author Stef Piatek
 */
public class TestIncrementalPathologyDuplicateResultSegment extends Hl7StreamEndToEndTestCase {
    public TestIncrementalPathologyDuplicateResultSegment() {
        super();
        hl7StreamFileNames.add("TestIncrementalPathologyDuplicateFactSegment.txt");
    }

    @Test
    @Transactional
    public void testObrFactsExist() {
        Encounter enc = encounterRepo.findEncounterByEncounter("7878787877");
        System.out.println(enc);
        List<PatientFact> facts = enc.getFacts();
        System.out.println(String.format(" FACTS x %s:", facts.size()));
        for (PatientFact pf : facts) {
            System.out.println(String.format("    FACT[TYPE=%s[%d]]:", pf.getFactType().getShortName(), pf.getFactType().getAttributeId()));

            List<PatientProperty> properties = pf.getProperties();
            if (properties != null) {
                System.out.println(String.format("        PROPERTIES x %s:", properties.size()));
                for (PatientProperty pp : properties) {
                    System.out.println("            " + pp.toString());
                }
            }
            List<PatientFact> childFacts = pf.getChildFacts();
            System.out.println(String.format("        CHILDFACTS x %s:", childFacts.size()));
            for (PatientFact chFact : childFacts) {
                System.out.println(String.format("            CFACT[TYPE=%s[%d]]:", chFact.getFactType().getShortName(), chFact.getFactType().getAttributeId()));
                List<PatientProperty> childProperties = chFact.getProperties();
                System.out.println(String.format("                CF PROPERTIES x %s:", childProperties.size()));
                for (PatientProperty ppp : childProperties) {
                    System.out.println("                   " + ppp.toString());
                }
            }
        }
        List<PatientFact> allFactsForEncounter = enc.getFacts();
        assertTrue(!allFactsForEncounter.isEmpty());
        Map<String, List<PatientFact>> allOrders = allFactsForEncounter.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_ORDER))
                .collect(Collectors.groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER).get(0).getValueAsString()));
        System.out.println("Keys: " + allOrders.keySet().toString());
        assertEquals(1, allOrders.size());

        List<PatientFact> childFacts001 = allOrders.get("22222222").get(0).getChildFacts();

        // check that pathology results have not been duplicated
        List<PatientFact> allTestResults001 = childFacts001.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        assertEquals(5, allTestResults001.size());

    }

    /**
     * Given that alanine transaminase is only in the second message, the value of 58 should exist
     */
    @Test
    @Transactional
    public void testNewResultIsCorrectValue() {
        List<PatientFact> allFactsForEncounter = encounterRepo.findEncounterByEncounter("7878787877").getFacts();
        Map<String, List<PatientFact>> allOrders = allFactsForEncounter.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_ORDER))
                .collect(Collectors.groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER).get(0).getValueAsString()));
        List<PatientFact> childFacts001 = allOrders.get("22222222").get(0).getChildFacts();
        List<PatientFact> allTestResults001 = childFacts001.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        PatientProperty alanineTransaminaseProperty = allTestResults001.get(2).getProperties().get(3);
        assertEquals(58, alanineTransaminaseProperty.getValueAsReal());
    }
}
