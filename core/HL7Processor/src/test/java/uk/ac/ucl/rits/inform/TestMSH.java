package uk.ac.ucl.rits.inform;

//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;

//import java.util.Vector;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.Message;
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

// NB despite the @Test annotation each test method name must begin with "test"


// parameterised tests https://www.tutorialspoint.com/junit/junit_parameterized_test.htm
//@RunWith(Parameterized.class) // <- doesn't work with our version of junit (4.10)
public class TestMSH extends TestCase {

    private PipeParser parser;
    private HapiContext context;
    private Message msg;
    private MSH msh;
    private MSHWrap wrapper;

	
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
            msg = parser.parse(generic_hl7);
            ADT_A01 adt_01 = (ADT_A01) parser.parse(msg.encode());
            msh = adt_01.getMSH();
            wrapper = new MSHWrap(msh);
        }
        catch (HL7Exception e) {
            e.printStackTrace();
            return;
        }

    }

    @Test
    // MSH-3
    public void testGetSendingApplication1() {
        String result = "";
        try {
            result = wrapper.getSendingApplication();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetSendingApplication1()");
            e.printStackTrace();
        }
        assertEquals(result, "UCLH4");
    }


    @Test
    // MSH-4
    public void testGetSendingFacility1() {
        String result = "";
        try {
            result = wrapper.getSendingFacility();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetSendingAFacility1()");
            e.printStackTrace();
        }
        assertEquals(result, "RRV30");
    }


    @Test
    // MSH-5
    public void testGetReceivingApplication1() {
        String result = "";
        try {
            result = wrapper.getReceivingApplication();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetReceivingApplication1()");
            e.printStackTrace();
        }
        assertEquals(result, "UCLH");
    }
    
    @Test
    // MSH-6
    public void testGetReceivingFacility1() {
        String result = "";
        try {
            result = wrapper.getReceivingFacility();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetReceivingFacility1()");
            e.printStackTrace();
        }
        assertEquals(result, "ZZ");
    }


    @Test
    // MSH-7
    public void testGetMessageTimestamp1() {
        String result = "";
        try {
            result = wrapper.getMessageTimestamp();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetMessageTimestamp1()");
            e.printStackTrace();
        }
        assertEquals(result, "201209211843");
    }


    @Test
    // MSH-9.1
    public void testGetMessageType1() {
        String result = "";
        try {
            result = wrapper.getMessageType();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetSessageType1()");
            e.printStackTrace();
        }
        assertEquals(result, "ADT");
    }

    @Test
    // MSH-9.2
    public void testGetTriggerEvent1() {
        String result = "";
        try {
            result = wrapper.getTriggerEvent();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetTriggerEvent1()");
            e.printStackTrace();
        }
        assertEquals(result, "A01");
    }


    @Test
    // MSH-10
    public void testGetMessageControlID1() {
        String result = "";
        try {
            result = wrapper.getMessageControlID();
        }
        catch (HL7Exception e) {
            System.out.println("Got exception in testGetMessageControlID1()");
            e.printStackTrace();
        }
        assertEquals(result, "PLW21221500942883310");
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