package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.query.NativeQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestIncrementalPathology extends Hl7StreamTestCase {
    public TestIncrementalPathology() {
        super();
        hl7StreamFileNames.add("GenericAdt/A01.txt");
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

        // .collect(Collectors.partitioningBy(f -> f.isValid()));

        //PatientFact pathOrder = allFactsForEncounter.get(AttributeKeyMap.PATHOLOGY_ORDER.getShortname());
        //assertNotNull(pathOrder);
        // XXX:
        List<PatientFact> childFacts001 = allOrders.get("94000001").get(0).getChildFacts();
        List<PatientFact> childFacts002 = allOrders.get("94000002").get(0).getChildFacts();

        // the order must have a child of type test result that has a battery code value of FBC
        // assertTrue(
        List<PatientFact> allTestResults001 = childFacts001.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        List<PatientFact> allTestResults002 = childFacts002.stream().filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        assertEquals(2, allTestResults001.size());
        assertEquals(17, allTestResults002.size());
        for (PatientFact pf : allTestResults001) {
            System.out.println("JES001: " + pf.toString());
        }
        for (PatientFact pf : allTestResults002) {
            System.out.println("JES002: " + pf.toString());
        }
                        
                                //.getValueAsString().equals("FBC")));
    }
}
