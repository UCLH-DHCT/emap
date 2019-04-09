package uk.ac.ucl.rits.inform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Vector;
import ca.uhn.hl7v2.model.v27.segment.PV1;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;


// https://www.tutorialspoint.com/junit/junit_environment_setup.htm
// https://examples.javacodegeeks.com/core-java/junit/junit-setup-teardown-example/

// For multi-class example see https://www.tutorialspoint.com/junit/junit_suite_test.htm 


import org.junit.Test;

import junit.framework.TestCase;
import uk.ac.ucl.rits.inform.hl7.Doctor;
import uk.ac.ucl.rits.inform.hl7.PV1Wrap;

// NB despite the @Test annotation each test method name must begin with "test"


// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestPV1 extends TestCase {

	//@Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
	
    // https://dzone.com/articles/mockito-basic-example-using-jdbc
    //@Mock
    /*private ResultSet mockrs;
    private DataSource mockds;
    private PreparedStatement mockps;
    private Connection mockconn;
    private HL7Processor mockproc;
	*/


    private PipeParser parser;
    private HapiContext context;
    private PV1 pv1;
    private PV1Wrap wrapper;

	/*@Before
    public void init(){
      //  MockitoAnnotations.initMocks(this);

      context = new DefaultHapiContext();
      ValidationContext vc = ValidationContextFactory.noValidation();
      context.setValidationContext(vc);

      // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
      CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
      context.setModelClassFactory(mcf);
      parser = context.getPipeParser(); //getGenericParser();


    }*/

    @Override
    public void setUp() throws Exception {
        //System.out.println("Setting it up in TestPV1!");

        // An example Atos-provided message
        String hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|||201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";

            context = new DefaultHapiContext();
            ValidationContext vc = ValidationContextFactory.noValidation();
            context.setValidationContext(vc);
      
            // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
            CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
            context.setModelClassFactory(mcf);
            parser = context.getPipeParser(); //getGenericParser();
            
            try {
                ADT_A01 adt_01 = (ADT_A01) parser.parse(hl7);
                pv1 = adt_01.getPV1();
                wrapper = new PV1Wrap(pv1);
            }
            catch (HL7Exception e) {
                e.printStackTrace();
                return;
            }

	/*
	mockrs = mock(ResultSet.class);
	mockps = mock (PreparedStatement.class);
	mockds = mock (DataSource.class);
	mockconn = mock(Connection.class);
	mockproc = mock(HL7Processor.class);

	assertNotNull(mockds);
	when(mockconn.prepareStatement(any(String.class))).thenReturn(mockps);
    	when(mockds.getConnection()).thenReturn(mockconn);
    	when(mockps.executeQuery()).thenReturn(mockrs);
	*/

    }


    @Test
    // PV1-3.1
    public void testGetCurrentWardCode1() {
        String result = "";
        try {
            result = wrapper.getCurrentWardCode();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetCurrentWardCode1()");
            e.printStackTrace();
        }
        assertEquals("H2HH", result);
    }


    @Test
    // PV1-3.2
    public void testGetCurrentRoomCode1() {
        String result = "";
        try {
            result = wrapper.getCurrentRoomCode();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetCurrentRoomCode1()");
            e.printStackTrace();
        }
        assertEquals("H203", result);
    }


    @Test
    // PV1-3.3
    public void testGetCurrentBed1() {

	/*        ResultSet rs = null;
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
	*/
        //System.out.println("Hello from a test function");
        String result = "";
        try {
            result = wrapper.getCurrentBed();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetCurrentBed1()");
            e.printStackTrace();
        }
        //System.out.println("debug test result = " + result);
        assertEquals("H203-11", result);
    }

    @Test
    // PV1-8
    public void testGetReferringDoctors1() {
        Doctor dr1 = null, dr2 = null;
        Vector<Doctor> vec = null; 
        try {
           vec = wrapper.getReferringDoctors();
        }
        catch(HL7Exception e) {
            System.out.println("Got exception in testGetReferringDoctors1()");
            e.printStackTrace();
        }
        dr1 = vec.get(0);
        dr2 = vec.get(1);
        assertEquals("Wrong dr1 consultant code", "397982", dr1.getConsultantCode());
        assertEquals("Wrong dr2 consultant code", "392275", dr2.getConsultantCode());
        assertEquals("Wrong dr1 surname", "LAWSON", dr1.getSurname());
        assertEquals("Wrong dr2 surname", "ISENBERG", dr2.getSurname());
        assertEquals("Wrong dr1 firstname", "M", dr1.getFirstname());
        assertEquals("Wrong dr2 firstname", "DAVID", dr2.getFirstname());
        assertEquals("Wrong dr1 middlename", null, dr1.getMiddlenameOrInitial());
        assertEquals("Wrong dr2 middlename", "J", dr2.getMiddlenameOrInitial());
        assertEquals("Wrong dr1 title", "DR", dr1.getTitle());
        assertEquals("Wrong dr2 title", "PROF", dr2.getTitle());
        // assertEquals("")getLocalCode()
    }


    @Test 
    // PV1-10
    public void testGetHospitalService1() throws HL7Exception {

        String result = "";
        try {
            result = wrapper.getHospitalService();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetHospitalService1()");
            e.printStackTrace();
        }
        assertEquals("41008", result);

    } 

    @Test 
    // PV1-14
    public void testGetAdmitSource1() throws HL7Exception {

        String result = "";
        try {
            result = wrapper.getAdmitSource();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetAdmitSource1()");
            e.printStackTrace();
        }
        assertEquals("19", result);

    }

    @Test 
    // PV1-18
    public void testGetPatientType1() throws HL7Exception {

        String result = "";
        try {
            result = wrapper.getPatientType();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetAPatientSource1()");
            e.printStackTrace();
        }
        assertEquals(null, result);

    }

    @Test 
    // PV1-19
    public void testGetVisitNumber1() throws HL7Exception {

        String result = "";
        try {
            result = wrapper.getVisitNumber();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetVisitNumber1()");
            e.printStackTrace();
        }
        assertEquals(null, result);

    }

    @Test 
    // PV1-44.1
    public void testGetAdmissionDateTime1() throws HL7Exception {
        assertEquals(
                Instant.parse("2012-09-21T17:40:00.00Z"),
                wrapper.getAdmissionDateTime());
    }

    @Test 
    // PV1-45.1
    public void testGetDischargeDateTime1() throws HL7Exception {
        assertEquals(
                null,
                wrapper.getDischargeDateTime());

    }


    @Override
    protected void tearDown() throws Exception {
        // System.out.println("Running: tearDown");
    }

}

/* original message from Atos

// An example Atos-provided message
        String hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|||201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";



*/