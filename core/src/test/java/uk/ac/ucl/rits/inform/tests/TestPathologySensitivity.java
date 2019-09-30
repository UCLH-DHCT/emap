package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

/**
 * Test adding results with antibiotic sensitivities included.
 * @author Jeremy Stein
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
public class TestPathologySensitivity extends Hl7StreamTestCase {
    public TestPathologySensitivity() {
        super();
        hl7StreamFileNames.add("PathologySensitivity.txt");
    }

    @Test
    @Transactional
    public void testOrder1Present() {
        List<PatientFact> orders = patientFactRepo.findAllPathologyOrdersByOrderNumber("93939393");
        assertEquals(1, orders.size());
        PatientFact ord = orders.get(0);
        assertEquals("93939393", ord.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER).get(0).getValueAsString());
        assertEquals("20V042424", ord.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_LAB_NUMBER).get(0).getValueAsString());
        List<PatientFact> results = ord.getChildFacts();
        // should be 2 results, each with 1 sensitivity order with 5 results
        assertEquals(2, results.size());
        List<PatientFact> sens1 = results.get(0).getChildFacts();
        List<PatientFact> sens2 = results.get(1).getChildFacts();
        assertEquals(5, sens1.size());
        assertEquals(5, sens2.size());
    }

    @Test
    @Transactional
    public void testOrder2Present() {
        List<PatientFact> orders = patientFactRepo.findAllPathologyOrdersByOrderNumber("93939395");
        assertEquals(1, orders.size());
        PatientFact ord = orders.get(0);
        assertEquals("93939395", ord.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER).get(0).getValueAsString());
        assertEquals("20V042424", ord.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_LAB_NUMBER).get(0).getValueAsString());
        List<PatientFact> results = ord.getChildFacts();
        // should be 5 microscopy results (no sensitivities)
        assertEquals(5, results.size());
    }
}
