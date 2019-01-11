// https://www.tutorialspoint.com/junit/junit_environment_setup.htm
// https://examples.javacodegeeks.com/core-java/junit/junit-setup-teardown-example/

// For multi-class example see https://www.tutorialspoint.com/junit/junit_suite_test.htm 


import org.junit.Test;
import static org.junit.Assert.*; 
import junit.framework.TestCase;
import org.json.simple.JSONObject; 
import uk.ac.ucl.rits.inform.*;
import static org.mockito.Mockito.*;

//import static org.mockito.Mockito.*;

// NB despite the @Test annotation each test method name must begin with "test"

/**
	Test class for HL7Processor::interrogate_json_object()
*/	

// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestJson extends TestCase {

    private HL7Processor mockproc;

    @Override
    public void setUp() throws Exception {
        //System.out.println("Setting it up!");
	mockproc = mock(HL7Processor.class);
    }


    @Test
    // Normal operation
    public void testijo1() {
        
	JSONObject jo = new JSONObject();
	jo.put("udshost", "192.168.1.1");
	jo.put("idshost", "192.168.1.1");
	jo.put("idsusername", "idstestuser");
	jo.put("udsusername", "udstestuser");
	jo.put ("idspassword", "password");
	jo.put("udspassword", "letmein");
	jo.put("debugging", "true");

	mockproc.interrogate_json_object(jo);	


    }




    @Override
    protected void tearDown() throws Exception {
        // System.out.println("Running: tearDown");
    }

}
