package uk.ac.ucl.rits.inform;


import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;

import org.junit.Test;

import junit.framework.TestCase;

// NB despite the @Test annotation each test method name must begin with "test"


// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestPID extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private PID pid;
    private PIDWrap wrapper;

	
    @Override
    public void setUp() throws Exception {
        System.out.println("**Setting it up in TestMSH!");

        // An example message, adapted from one provided by Atos
        // NB This is a generic string for testing. Atos A01 example messages, for instance, do not have field MSH-5 populated.
        // So there will likely be content in fields below which, for a given message type, would actually be blank.
        String generic_hl7 = "MSH|^~\\&|UCLH4^PMGL^ADTOUT|RRV30|UCLH|ZZ|201209211843||ADT^A01|PLW21221500942883310|P|2.2|||AL|NE\r"
            + "ZUK|Q12|5CF|1|||||||N  N||12||||||||||||||||B83035^2.16.840.1.113883.2.1.4.3|G9014646^2.16.840.1.113883.2.1.4.2|U439966^2.16.840.1.113883.2.1.3.2.4.11||41008\r";

        context = new DefaultHapiContext();
        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);
      
        // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
        context.setModelClassFactory(mcf);
        parser = context.getPipeParser(); //getGenericParser();
            
        try {
            ADT_A01 adt_01 = (ADT_A01) parser.parse(generic_hl7);
            pid = adt_01.getPID();
            wrapper = new PIDWrap(pid);
        }
        catch (HL7Exception e) {
            e.printStackTrace();
            return;
        }

    }

    @Test
    // PID-5.1
    public void testGetPatientFamilyName1() {
        String result = "";
        try {
            result = wrapper.getPatientFamilyName();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetPatientFamilyName1()");
            e.printStackTrace();
        }
        assertEquals("INTERFACES", result);
    }


    @Test
    // PID-5.2
    public void testGetPatientGivenName1() {
        String result = "";
        try {
            result = wrapper.getPatientGivenName();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetPatientGivenName1()");
            e.printStackTrace();
        }
        assertEquals("Amendadmission", result);
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