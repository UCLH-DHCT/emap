// Engine.java
//
// Given an HL7 message, take appropriate action based on what that message is.
//
// Matthew Gillman, UCL, 29th August 2018

package uk.ac.ucl.rits.inform;

import java.awt.event.PaintEvent;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.model.Message;

// HL7 2.2 only
//import ca.uhn.hl7v2.model.v22.message.ADT_A01;
//import ca.uhn.hl7v2.model.v22.message.ADT_A02; // was v25
import ca.uhn.hl7v2.model.v22.segment.PID;
import ca.uhn.hl7v2.model.v22.datatype.PN;
// HL7 2.3 only
//import ca.uhn.hl7v2.model.v23.message.ADT_A01;
//import ca.uhn.hl7v2.model.v23.message.ADT_A02; 

// etc.

public class Engine {

    public Engine(HapiContext c, Parser p) {
        context = c;
        parser = p;
        System.out.println("Engine created successfully");
    }

    public void processMessage(Message msg) {
        String ver = msg.getVersion();
        System.out.println("Engine: version is " + ver);

        if (ver.equals("2.2")) {

            System.out.println("HEY! Got " + ver);
            // All 2.2 ASDT messages A01 to A37 have PID segment EXCEPT A20 (bed status update)
            // Maybe sometimes some of the PID info will be unknown??

            // From https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/ExampleSuperStructures.html
            // ADT A01: Admit a Patient
            if (msg instanceof /*ADT_A01*/ ca.uhn.hl7v2.model.v22.message.ADT_A01) {
                System.out.println("Got an ADT_A01");//processAdtA01((ADT_A01) msg);
                //ca.uhn.hl7v2.model.v22.segment.PID pid = msg.getPID();
                ca.uhn.hl7v2.model.v22.message.ADT_A01 a = (ca.uhn.hl7v2.model.v22.message.ADT_A01) msg;
                PN patientName = a.getPID().getPatientName();
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
                ca.uhn.hl7v2.model.v22.message.ADT_A04 a = (ca.uhn.hl7v2.model.v22.message.ADT_A04) msg;
                ca.uhn.hl7v2.model.v22.segment.PID pid = a.getPID(); // = msg.getPID();
                PN patientName = pid.getPatientName();
                System.out.println ("\n^^^^ patient name is " + patientName.getFamilyName().getValue());

                /*Class c = msg.getClass();
                for (Method method : c.getDeclaredMethods()) {
                    if (method.getAnnotation(PostConstruct.class) != null) {
                        System.out.println(method.getName());
                    }
                }*/

            }
        }

        else System.out.println("Another message type");

    }

    private void writeToDatabase() {

    }

    private HapiContext context;
    private Parser parser;

}