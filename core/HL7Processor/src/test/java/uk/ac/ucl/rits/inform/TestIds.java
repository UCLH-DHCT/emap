package uk.ac.ucl.rits.inform;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.ucl.rits.inform.ids.IdsMasterRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestIds {

    @Autowired
    private IdsMasterRepository idsMasterRepository;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        System.out.println("what is going on here?");
        idsMasterRepository.findById(42);
        System.out.println(idsMasterRepository.count());
        System.out.println("shrug");
    }

}
