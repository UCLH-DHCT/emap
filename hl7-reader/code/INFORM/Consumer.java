// Consumer.java
//
// Utility to ingest HL7 messages as strings (for later parsing)
// either from a file or from the Immutable Data Store (IDS).
//
// Matthew Gillman
// 20th August 2018

package uk.ac.ucl.rits.inform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.sun.org.apache.xerces.internal.impl.dv.DatatypeException;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.validation.ValidationException;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

public class Consumer {

    public static void HelpMessage() {

            String str = "Usage:\n"
                + "Consumer [ -h | -f filename | -d databasefile -i last_unid_processed ]\n ";
            System.out.println(str);
            System.exit(0);

    }

    // Based on the ReadMessagesFromFile class in the HAPI examples
    public static void process_file(String filename) {

        File file = new File(filename);
    	InputStream is;
	    try {
            is = new FileInputStream(file);
            is = new BufferedInputStream(is);
            //Hl7InputStreamMessageStringIterator iter2 = new Hl7InputStreamMessageStringIterator(is); 
            Hl7InputStreamMessageIterator iter2 = new Hl7InputStreamMessageIterator(is, context); 
            iter2.setIgnoreComments(true);
            
            while (iter2.hasNext()) {
                
                //String next = iter2.next();
                
                // Do something with the message. The iterator strips off the MSH and EVN segments.
                //System.out.println("\nNext message is:******************\n" + next);
                Message hapiMsg = iter2.next();
                String ver = hapiMsg.getVersion();
                System.out.println("version is " + ver);
                /*try {
                    // The parse method performs the actual parsing
                    hapiMsg = p.parse(next);
                } catch (EncodingNotSupportedException e) {
                    e.printStackTrace();
                    return;
                } catch (HL7Exception h) {
                    h.printStackTrace();
                    return;
                } */
                
                System.exit(1);

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

        context = new DefaultHapiContext();
        context.setValidationContext(ValidationContextFactory.noValidation());
        p = context.getGenericParser();


        // Not very elegant.
        // Also want a utility to tell us last unid processed (maybe stored as json)
        // which we may (or may not) take notice of.
        if (arglen > 4) {
            if (args[0].equals("-d") && args[2].equals("-i")) {
                dbconn = args[1];
                last_unid = Long.parseLong(args[3]);
                // read records from db
            }
            else if (args[0].equals("-i") && args[2].equals("-d")) {
                dbconn = args[3];
                last_unid = Long.parseLong(args[1]);
                // read records from db
            }
            else HelpMessage();

        }
        else if (arglen > 2) {
            if (args[0].equals("-f")) {
               filename = args[1];
               process_file(filename);
            }
            else HelpMessage();
        }
        else if (arglen > 0) {
            if (args[0].equals("-h")) {
                HelpMessage();
            }
        }
        else HelpMessage();


    }

    private static HapiContext context;

    // Move elsewhere:
    private static Parser p;
}
