package uk.ac.ucl.rits.inform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

// https://www.tutorialspoint.com/junit/junit_environment_setup.htm
// https://examples.javacodegeeks.com/core-java/junit/junit-setup-teardown-example/

// For multi-class example see https://www.tutorialspoint.com/junit/junit_suite_test.htm 


import org.junit.Test;

import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.hl7.HL7Processor;

// NB despite the @Test annotation each test method name must begin with "test"


// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestNull extends TestCase {

	//@Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
	
    // https://dzone.com/articles/mockito-basic-example-using-jdbc
    //@Mock
    private ResultSet mockrs;
    private DataSource mockds;
    private PreparedStatement mockps;
    private Connection mockconn;
    private HL7Processor mockproc;


	// @Before
    public void init(){
      //  MockitoAnnotations.initMocks(this);
    }

    @Override
    public void setUp() throws Exception {
        //System.out.println("Setting it up!");

	mockrs = mock(ResultSet.class);
	mockps = mock (PreparedStatement.class);
	mockds = mock (DataSource.class);
	mockconn = mock(Connection.class);
	mockproc = mock(HL7Processor.class);

	assertNotNull(mockds);
	when(mockconn.prepareStatement(any(String.class))).thenReturn(mockps);
    when(mockds.getConnection()).thenReturn(mockconn);
    when(mockps.executeQuery()).thenReturn(mockrs);
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
    // Test with an empty string
	public void testgnr2() {
        
        String str = "";
        boolean result = false;
        try {
            result = mockproc.got_null_result(mockrs, str);
        }
        catch (SQLException e) {
            System.out.println("Got exception");
            e.printStackTrace();
        }
	    assertTrue(result);

    }
    

    @Test
    // Test with a non-empty string
    public void testgnr3() {

        String str = "I am not empty";
        boolean result = false;
        try {
            result = mockproc.got_null_result(mockrs, str);
        }
        catch (SQLException e) {
            System.out.println("Got exception");
            e.printStackTrace();
        }
	    assertFalse(result);
    }


    @Override
    protected void tearDown() throws Exception {
        // System.out.println("Running: tearDown");
    }

}
