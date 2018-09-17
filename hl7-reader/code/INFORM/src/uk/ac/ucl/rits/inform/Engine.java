// Engine.java
//
// Given an HL7 message, take appropriate action based on what that message is.
//
// Matthew Gillman, UCL, 29th August 2018

package uk.ac.ucl.rits.inform;

import java.awt.event.PaintEvent;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
//import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.HL7Exception;

// HL7 2.2 only
//import ca.uhn.hl7v2.model.v22.message.ADT_A01;
//import ca.uhn.hl7v2.model.v22.message.ADT_A02; // was v25
import ca.uhn.hl7v2.model.v22.segment.PID;
import ca.uhn.hl7v2.model.v22.datatype.PN;
//import ca.uhn.hl7v2.model.v22.datatype.XPN;
// HL7 2.3 only
//import ca.uhn.hl7v2.model.v23.message.ADT_A01;
//import ca.uhn.hl7v2.model.v23.message.ADT_A02; 

// etc.

public class Engine {

    public Engine(HapiContext c, PipeParser p) {
        context = c;
        parser = p;
        System.out.println("Engine created successfully");
    }

    public void processMessage(Message msg) {
        String ver = msg.getVersion();
        System.out.println("Engine: version is " + ver);

        ///if (ver.equals("2.2")) {

            System.out.println("HEY! Got " + ver);
            // All 2.2 ASDT messages A01 to A37 have PID segment EXCEPT A20 (bed status update)
            // Maybe sometimes some of the PID info will be unknown??

            // From https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/ExampleSuperStructures.html
            // ADT A01: Admit a Patient
            if (/*msg instanceof ca.uhn.hl7v2.model.v22.message.ADT_A01 
                || msg instanceof ca.uhn.hl7v2.model.v23.message.ADT_A01
                || msg instanceof ca.uhn.hl7v2.model.v24.message.ADT_A01
                || */msg instanceof ca.uhn.hl7v2.model.v27.message.ADT_A01
                // etc
            ) {
                System.out.println("Got an ADT_A01");//processAdtA01((ADT_A01) msg);
                try {
                    process_ADT_01(msg);
                }
                catch (HL7Exception e)  {
                    e.printStackTrace();
                }
                //ca.uhn.hl7v2.model.v22.segment.PID pid = msg.getPID();
                
            } 
        
            // ADT A02: Transfer a patient
            else if (msg instanceof /*ADT_A02*/ ca.uhn.hl7v2.model.v22.message.ADT_A02) {
                System.out.println("Got an ADT_A02!!"); //processAdtA02((ADT_A02) msg);

            }

            // ADT A03: Discharge a patient

            // ADT A04: Register a Patient
            else if (msg instanceof ca.uhn.hl7v2.model.v22.message.ADT_A04) {
                System.out.println("Got an ADT_A04!!");

                // PID-3 patient ID (or list in later versions)
                // PID-4 alternate patient ID
                ////ca.uhn.hl7v2.model.v22.message.ADT_A04 a = (ca.uhn.hl7v2.model.v22.message.ADT_A04) msg;
               // ca.uhn.hl7v2.model.v22.segment.PID pid = a.getPID(); // = msg.getPID();
               // PN patientName = pid.getPatientName();
               // System.out.println ("\n^^^^ patient name is " + patientName.getFamilyName().getValue());

                /*Class c = msg.getClass();
                for (Method method : c.getDeclaredMethods()) {
                    if (method.getAnnotation(PostConstruct.class) != null) {
                        System.out.println(method.getName());
                    }
                }*/

            }
        ///}

        else System.out.println("Another message type");

    }

    // process_XXX_YY() functions
    //
    // Try and abstract out common functionality across HL7 versions as much as possible.
    // Unfortunately we cannot use dynamic binding; e.g. the AbstractMessage class and Message
    // interface do not have the function getPID(). So we cannot say things like:
    //      ca.uhn.hl7v2.model.AbstractMessage adt_01;
    //      adt_01 = ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
    //      PN patientName = adt_01.getPID().getPatientName();
    // as we will get a "cannot find symbol" error. Instead, the way to handle multiple HL7 versions
    // is by using the parser:
    // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
    // and  CanonicalModelClassFactory set to latest version we wish to support
    private void process_ADT_01 (Message msg) throws HL7Exception {
        /* cc */ //Message adt_01;

        // The parser parses the v2.3 message to a "v25" structure
        //ca.uhn.hl7v2.model.v27.message.ADT_ORU_R01 msg = (ca.uhn.hl7v2.model.v25.message.ORU_R01) parser.parse(v23message);
        ca.uhn.hl7v2.model.v27.message.ADT_A01 adt_01 = (ca.uhn.hl7v2.model.v27.message.ADT_A01) parser.parse(msg.encode());
        /*
        if (msg instanceof ca.uhn.hl7v2.model.v22.message.ADT_A01) {
            ca.uhn.hl7v2.model.v22.message.ADT_A01 a = (ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
            adt_01 = a;
        }      
        else if (msg instanceof ca.uhn.hl7v2.model.v23.message.ADT_A01) {
            ca.uhn.hl7v2.model.v23.message.ADT_A01 a = (ca.uhn.hl7v2.model.v23.message.ADT_A01) msg;
            adt_01 = a;
        }      
        else if (msg instanceof ca.uhn.hl7v2.model.v24.message.ADT_A01) {
            ca.uhn.hl7v2.model.v24.message.ADT_A01 a = (ca.uhn.hl7v2.model.v24.message.ADT_A01) msg;
            adt_01 = a;
        } */
        // etc. 

        //ca.uhn.hl7v2.model.v22.message.ADT_A01 a = (ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
        //PN patientName = adt_01.getPID().getPatientName();
        ca.uhn.hl7v2.model.v27.segment.PID pid = adt_01.getPID();
        ca.uhn.hl7v2.model.v27.datatype.XPN xpn[] = pid.getPatientName(); 
        System.out.println("++++++ patientName is " + xpn[0] + "++:-)+++++");

    }

    private void process_ADT_04 () {}

    private void writeToDatabase() {

    }

    private HapiContext context;
    private PipeParser parser;

}