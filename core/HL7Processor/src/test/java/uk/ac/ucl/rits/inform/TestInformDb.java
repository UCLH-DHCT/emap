package uk.ac.ucl.rits.inform;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ca.uhn.hl7v2.HL7Exception;
import uk.ac.ucl.rits.inform.hl7.AdtWrapMock;
import uk.ac.ucl.rits.inform.informdb.Encounter;

/**
 * @author Jeremy Stein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestInformDb {
    @Autowired
    private InformDbOperations dbt;

    /**
     * Add N encounters, then check that the encounter count has increased by N.
     * @throws HL7Exception if HAPI does
     */
    @Test
    public void testAddEncounters() throws HL7Exception {
        System.out.println("test?");
        long beforeEncounters = dbt.countEncounters();
        long numEncounters = 100;
        for (int i = 0; i < numEncounters; i++) {
            Encounter enc = dbt.addEncounter(new AdtWrapMock());
            //System.out.println("test: " + enc.toString());
        }
        long afterEncounters = dbt.countEncounters();
        long actualNewEncounters = afterEncounters - beforeEncounters;
        String failureString = String.format("Actual: before = %d, after = %d, difference = %d", beforeEncounters,
                afterEncounters, actualNewEncounters);
        System.out.println(failureString);
        assertEquals(failureString, numEncounters, actualNewEncounters);
    }

}
