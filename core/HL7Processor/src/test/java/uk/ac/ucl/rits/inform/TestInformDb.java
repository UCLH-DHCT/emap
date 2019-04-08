/**
 * 
 */
package uk.ac.ucl.rits.inform;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.ucl.rits.inform.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.informdb.Encounter;

/**
 * @author jeremystein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestInformDb {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Autowired
    private InformDbOperations dbt;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Add N encounters, then check that the encounter count has increased by N
     */
    @Test
    public void testAddEncounters() {
        System.out.println("test?");
        long beforeEncounters = dbt.countEncounters();
        long numEncounters = 100;
        for (int i = 0; i < numEncounters; i++) {
            Encounter enc = dbt.addEncounter(new AdtWrap());
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
