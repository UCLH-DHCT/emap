// https://www.tutorialspoint.com/junit/junit_environment_setup.htm
// https://examples.javacodegeeks.com/core-java/junit/junit-setup-teardown-example/

// For multi-class example see https://www.tutorialspoint.com/junit/junit_suite_test.htm 

// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) <- doesn't work with our version of junit (4.10)

import org.junit.Test;
import static org.junit.Assert.*; 
import junit.framework.TestCase;

// NB despite the @Test annotation each test method name must begin with "test"

public class TestConvertTimestamp extends TestCase {

    protected int v1, v2, v3;
    protected HL7Processor proc;


    @Override
    public void setUp() {
    //    System.out.println("Setting it up!");
        v1 = 17; v2 = 13;
        proc = new HL7Processor();
    }

    /////////////////////////////////
    // convert_timestamp tests
    // (HL7 to Postgres)
    /////////////////////////////////

    @Test
    // Null input string
    public void testct0() {
        String hl7 = "";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(jdbc, "NULL");
    }

    @Test
    // Expect return value same as input
    public void testct1() {
        String hl7 = "null::timestamp";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(hl7, jdbc);
    }

    @Test
    // Expect return value same as input (input already in Postgres format)
    public void testct2() {
        String hl7 = "2018-10-03 14:18:07.7618";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(hl7, jdbc);
    }

    @Test
    // Expect return value same as input (input already in Postgres format)
    public void testct2a() {
        String hl7 = "2018-10-03 14:18:07";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(hl7, jdbc);
    }

    @Test
    // An HL7 timestamp with sub-second precision
    public void testct3() {
        String hl7 = "20181003141807.7618";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(jdbc, "2018-10-03 14:18:07.7618");
    }

    @Test
    // HL7 timestamp with per-second precision
    public void testct4() {
        String hl7 = "20181003141807";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(jdbc, "2018-10-03 14:18:07");
    }

    @Test
    // HL7 timestamp with per-minute precision
    public void testct5() {
        String hl7 = "201810031418";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(jdbc, "2018-10-03 14:18:00");
    }

    @Test
    // HL7 timestamp with per-hour precision
    public void testct6() {
        String hl7 = "2018100314";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(jdbc, "2018-10-03 14:00:00");
    }

    @Test
    // HL7 timestamp with date information only
    public void testct7() {
        String hl7 = "20181003";
        String jdbc = HL7Processor.convert_timestamp(hl7);
        assertEquals(jdbc, "2018-10-03 00:00:00");
    }


    @Override
    protected void tearDown() throws Exception {
        // System.out.println("Running: tearDown");   
    }
}
