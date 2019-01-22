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

@SuppressWarnings("unchecked") // see https://stackoverflow.com/questions/53225896/jsonobject-unchecked-call-to-putk-v-as-a-member-of-the-raw-type-java-util-hash

// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestJson {

    private HL7Processor mockproc = mock(HL7Processor.class);


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

    @Test 
    // Empty JSONObject
    // check_json_value (JSONObject jo, String varname, String key)
    public void testcjv1() {
        JSONObject jo = new JSONObject();
        Object[] objarray = mockproc.check_json_value(jo, "udshost", "udshost");
        boolean b = ((Boolean)objarray[0]).booleanValue();
        assertFalse(b);
    }

    @Test
    // Normal operation
    public void testcjv2() {
        JSONObject jo = new JSONObject();
        jo.put("udshost", "192.168.1.1");
        Object[] objarray = mockproc.check_json_value(jo, "udshost", "udshost");
        boolean b = ((Boolean)objarray[0]).booleanValue();
        assertTrue(b);
    }

    @Test
    // Put one key/value into JSONObject and ask for another
    public void testcjv3() {
        JSONObject jo = new JSONObject();
        jo.put("udshost", "192.168.1.1");
        Object[] objarray = mockproc.check_json_value(jo, "idsusername", "idsusername");
        boolean b = ((Boolean)objarray[0]).booleanValue();
        assertFalse(b);
    }

    @Test
    // Empty string query
    public void testcjv4() {
        JSONObject jo = new JSONObject();
        jo.put("udshost", "192.168.1.1");
        Object[] objarray = mockproc.check_json_value(jo, "", "");
        boolean b = ((Boolean)objarray[0]).booleanValue();
        assertFalse(b);
    }


}
