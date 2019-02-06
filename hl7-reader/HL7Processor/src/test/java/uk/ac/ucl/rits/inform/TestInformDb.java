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

import uk.ac.ucl.rits.inform.informdb.Encounter;

/**
 * @author jeremystein
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
//@EnableAutoConfiguration  
//@Profile("test")
//@AutoConfigureMockMvc
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
    private DBTester dbt;

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

    @Test
    public void test() {
        System.out.println("test?");
        long numEncounters = 100;
        for (int i = 0; i < numEncounters; i++) {
            Encounter enc = dbt.addEncounter(new A01Wrap());
            System.out.println("test: " + enc.toString());
        }
        long actualEncounters = dbt.countEncounters();
        assertEquals(numEncounters, actualEncounters);
        // fail("Not yet implemented");
    }

}
