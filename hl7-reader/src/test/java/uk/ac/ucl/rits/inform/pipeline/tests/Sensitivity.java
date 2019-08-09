package uk.ac.ucl.rits.inform.pipeline.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
public class Sensitivity extends Hl7StreamTestCase {
    public Sensitivity() {
        super();
        hl7StreamFileNames.add("PathologySensitivity.txt");
    }

    @Test
    @Transactional
    public void testSensitivityPresent() {
        List<PatientFact> orders = patientFactRepo.findAllPathologyOrdersByOrderNumber("93939393");
        assertEquals(1, orders.size());
        PatientFact ord = orders.get(0);
        assertEquals("20V042424", ord.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_LAB_NUMBER).get(0).getValueAsString());
        List<PatientFact> childFacts = ord.getChildFacts();
        assertEquals(1, childFacts.size());
    }
}
