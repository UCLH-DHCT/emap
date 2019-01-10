// https://www.tutorialspoint.com/junit/junit_environment_setup.htm
// https://examples.javacodegeeks.com/core-java/junit/junit-setup-teardown-example/

// For multi-class example see https://www.tutorialspoint.com/junit/junit_suite_test.htm 

// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) <- doesn't work with our version of junit (4.10)

import org.junit.Test;
import static org.junit.Assert.*; 
import junit.framework.TestCase;
import java.sql.*;
import uk.ac.ucl.rits.inform.*;

import static org.mockito.Mockito.*;

// NB despite the @Test annotation each test method name must begin with "test"

public class TestNull extends TestCase {

	//@Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

	//@Mock
	//HL7Processor mockproc;


	// @Before
    public void init(){
      //  MockitoAnnotations.initMocks(this);
    }

    @Override
    public void setUp() {
        /*System.out.println("Setting it up!");
        v1 = 17; v2 = 13;
        proc = new HL7Processor();*/
    }


    /////////////////////////////////
    // got_null_result tests
    /////////////////////////////////

    @Test
    // Expect null result
    public void testgnr1() {
        ResultSet rs = null;
        String str = "hello";
        boolean result = false;
        try {
            result = HL7Processor.got_null_result(rs, str);
        }
        catch (SQLException e) {
            System.out.println("Got exception");
            e.printStackTrace();
        }
	    assertTrue(result);
    }


	@Test
	public void testmock() {
		HL7Processor mockproc = mock(HL7Processor.class);
	}

    @Override
    protected void tearDown() throws Exception {
        // System.out.println("Running: tearDown");
    }

}
