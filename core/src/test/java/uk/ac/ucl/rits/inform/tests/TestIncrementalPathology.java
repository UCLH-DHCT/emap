package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Messages come through giving progressively more results.
 * New results should be added taking care to avoid duplicating existing ones.
 * @author Jeremy Stein
 */
public class TestIncrementalPathology extends Hl7StreamEndToEndTestCase {
    public TestIncrementalPathology() {
        super();
        hl7StreamFileNames.add("IncrementalPathology.txt");
    }

    @Test
    @Transactional
    public void testSomething() {
        Encounter enc = encounterRepo.findEncounterByEncounter("123412341234");
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
        List <PatientFact> allFactsForEncounter = enc.getFacts();
        assertTrue(!allFactsForEncounter.isEmpty());
        Map<String, List<PatientFact>> allOrders = allFactsForEncounter.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_ORDER))
                .collect(Collectors.groupingBy(pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER).get(0).getValueAsString()));
        System.out.println("Keys: " + allOrders.keySet().toString());
        assertEquals(2, allOrders.size());

        List<PatientFact> childFacts001 = allOrders.get("94000001").get(0).getChildFacts();
        List<PatientFact> childFacts002 = allOrders.get("94000002").get(0).getChildFacts();

        // check that pathology results have not been duplicated
        List<PatientFact> allTestResults001 = childFacts001.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        List<PatientFact> allTestResults002 = childFacts002.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        assertEquals(2, allTestResults001.size());
        assertEquals(17, allTestResults002.size());

        // check that most recent message result is used
        PatientProperty monocyteProperty =  allTestResults002.get(1).getProperties().get(3);
        assertEquals(0.5, monocyteProperty.getValueAsReal());
    }
}
