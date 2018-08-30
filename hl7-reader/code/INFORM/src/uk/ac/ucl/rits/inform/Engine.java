// Engine.java
//
// Given an HL7 message, take appropriate action based on whatthat message is.
//
// Matthew Gillman, UCL, 29th August 2018

package uk.ac.ucl.rits.inform;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.model.Message;

import ca.uhn.hl7v2.model.v22.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A02;

public class Engine {

    public Engine(HapiContext c, Parser p) {
        context = c;
        parser = p;
        System.out.println("Engine created successfully");
    }

    public void processMessage(Message msg) {
        String ver = msg.getVersion();
        System.out.println("Engine: version is " + ver);

        // From https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/ExampleSuperStructures.html
        if (msg instanceof ADT_A01) {
            System.out.println("Got an ADT_A01");//processAdtA01((ADT_A01) msg);
        } else if (msg instanceof ADT_A02) {
            System.out.println("Got an ADT_A02!!"); //processAdtA02((ADT_A02) msg);
        }
        else System.out.println("Another message type");

    }

    private void writeToDatabase() {

    }

    private HapiContext context;
    private Parser parser;

}