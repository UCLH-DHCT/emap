// https://www.tutorialspoint.com/junit/junit_environment_setup.htm
// https://examples.javacodegeeks.com/core-java/junit/junit-setup-teardown-example/
import org.junit.Test;
import static org.junit.Assert.*; //assertEquals;

import java.beans.Transient;

import junit.framework.TestCase;

public class TestHL7Processor extends TestCase {

protected int v1, v2, v3;
protected HL7Processor proc;

//@Before // setup()
//    public void before()  {
@Override
public void setUp() {
    System.out.println("Setting it up!");
    v1 = 17; v2 = 13;
    proc = new HL7Processor();
}

@Test
public void testAdd() {
    String str = "Junit is working fine";
    assertEquals("Junit is working fine",str);
}

@Test
public void testSum() {
	double result = v1 + v2;
	assertTrue(result == 30);
}
    
@Test
public void test_convert_timestamp() {
    String hl7 = "null::timestamp";
    String jdbc = HL7Processor.convert_timestamp(hl7);
    assertEquals(hl7, jdbc);
}


@Override
protected void tearDown() throws Exception {
    System.out.println("Running: tearDown");
    
    }
}
