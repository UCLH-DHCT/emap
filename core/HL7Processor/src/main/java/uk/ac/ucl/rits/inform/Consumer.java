package uk.ac.ucl.rits.inform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

import ca.uhn.hl7v2.validation.ValidationContext;

/**
 * 
 * Consumer.java
 * 
 * Utility to ingest HL7 messages as strings (for later parsing)
 * either from a file or from the Immutable Data Store (IDS).
 * 
 * Matthew Gillman
 * 20th August 2018
 */
public class Consumer {

    /**
     * Output a usage message.
     */
    public void helpMessage() {

            String str = "Usage:\n"
                + "Consumer [ -h | -f filename | -d databasefile -i last_unid_processed ]\n ";
            System.out.println(str);
            System.exit(0);

    }

    /**
     * Constructor
     */
    public Consumer() {
    	
    }
    
    // Based on the ReadMessagesFromFile class in the HAPI examples
    /**
     * Open a text file and read in (consume) HL7 messages iteratively.
     * Then use the Engine class to process each message.
     * 
     * @param filename A file of HL7 messages
     */
    public void process_file(String filename) {

        File file = new File(filename);
    	InputStream is;
	    try {
            is = new FileInputStream(file);
            is = new BufferedInputStream(is);
            Hl7InputStreamMessageIterator iter2 = new Hl7InputStreamMessageIterator(is, context); 
            iter2.setIgnoreComments(true);
            int count = 0;
            while (iter2.hasNext()) {
                count++;
                //String next = iter2.next();
                System.out.println("*************** Next (" + count + ")*****************");
                // Do something with the message. The iterator strips off the MSH and EVN segments.
                //System.out.println("\nNext message is:******************\n" + next);
                Message msg = iter2.next();

                engine.processMessage(msg);
    
                ////String msgType = hapiMsg.getMessageType().getMessageType().getValue();
                ////String msgTrigger = hapiMsg.getMessageType().getTriggerEvent().getValue();

                // Prints
                ///System.out.println("\n\nSpecific: " + msgType + " " + msgTrigger);
                
                // From https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/ExampleSuperStructures.html
                /*if (msg instanceof ADT_A01) {
                    System.out.println("ADT_A01");//processAdtA01((ADT_A01) msg);
                } else if (msg instanceof ADT_A02) {
                    //processAdtA02((ADT_A02) msg);
                            }
                else System.out.println("Another message type"); 
                            */
                ////System.exit(1);

            }
	    }
	    catch (FileNotFoundException fnf) {
		    System.out.println("\nError, could not find file " + filename);
		    System.exit(1);
	    }
      	catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        } 

    }


    /**
     * Query the IDS for HL7 messages and then process each message.
     * <p>
     * TODO Not yet implemented.
     * Likely Args - initial UNID (1 onwards) and optional final UNID
     * If both present, get records between initial and final (both ends included)
     * If just initial present, get all records after and including this UNID.
     */
    public void query_IDS() {


        // for each record: engine.processMessage()
    }


    /**
     * main function
     * 
     * Reads HL7 messages in, either from a file or (TODO) the IDS.
     * <p>
     * As HL7 messages may be from different HL7 versions, but later versions are
     * backwards-compatible, we use HAPI's "CanonicalModelClassFactory"
     * to set them all to the same version.
     * For details, see:
     * https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
     * 
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        System.out.println("Hello, World");

        // We read HL7 messages in either from a file (for testing and development)
        // or from the Postgres SQL IDS (in which case we want to know the last UNID processed)    

        int arglen = args.length;
        for (int i = 0; i < arglen; i++) {
            System.out.println ("Arg " + i + " is " + args[i]);
        }

        String dbconn;
        long last_unid = 0; // Must be >= 1
        String filename;

        Consumer c = new Consumer();
        c.context = new DefaultHapiContext();
        ValidationContext vc = ValidationContextFactory.noValidation();
        c.context.setValidationContext(vc);

        // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
        c.context.setModelClassFactory(mcf);
        c.p = c.context.getPipeParser(); //getGenericParser();
        c.engine = new Engine(c.context, c.p);

        // Not very elegant. There must be a standard way of doing this
        // Also want a utility to tell us last unid processed (maybe stored as json)
        // which we may (or may not) take notice of.
        if (arglen >= 4) {
            if (args[0].equals("-d") && args[2].equals("-i")) {
                dbconn = args[1];
                last_unid = Long.parseLong(args[3]);
                // read records from db starting with UNID i
            }
            else if (args[0].equals("-i") && args[2].equals("-d")) {
                dbconn = args[3];
                last_unid = Long.parseLong(args[1]);
                // read records from db starting with UNID i
            }
            else c.helpMessage();

        }
        else if (arglen >= 2) {
            if (args[0].equals("-f")) {
               filename = args[1];
               c.process_file(filename);
            }
            else c.helpMessage();
        }
        else if (arglen > 0) {
            if (args[0].equals("-h")) {
                c.helpMessage();
            }
        }
        else c.helpMessage();


    }

    private DefaultHapiContext context;

    private PipeParser p;

    private Engine engine;

} // End (class Consumer)
