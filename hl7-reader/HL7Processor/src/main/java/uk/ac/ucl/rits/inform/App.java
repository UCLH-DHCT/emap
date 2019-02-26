package uk.ac.ucl.rits.inform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import uk.ac.ucl.rits.inform.informdb.Encounter;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop(DBTester dbt) {
        return (args) -> {
            System.out.println("hi");

            long startTimeMillis = System.currentTimeMillis();
            HapiContext context = new DefaultHapiContext();

            ValidationContext vc = ValidationContextFactory.noValidation();
            context.setValidationContext(vc);

            // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
            CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
            context.setModelClassFactory(mcf);
            PipeParser parser = context.getPipeParser(); // getGenericParser();

            File file = null;
            try {
                file = new File(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(file.getAbsolutePath() + " " + file.exists());
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Hl7InputStreamMessageIterator hl7iter = new Hl7InputStreamMessageIterator(is, context);
            hl7iter.setIgnoreComments(true);
            int count = 0;
            while (hl7iter.hasNext()) {
                count++;
                System.out.println("*************** Next (" + count + ")*****************");
                // Do something with the message. The iterator strips off the MSH and EVN
                // segments.
                Message msg = hl7iter.next();
                processHL7(dbt, parser, msg);
            }
            long endCurrentTimeMillis = System.currentTimeMillis();
            System.out.println(String.format("done, %.0f secs", (endCurrentTimeMillis - startTimeMillis) / 1000.0));
            context.close();

        };
    }

    private void processHL7(DBTester dbt, PipeParser parser, Message msg) throws HL7Exception {

        System.out.println("Engine: version is " + msg.getVersion());

        if (msg instanceof ADT_A01) {
            System.out.println("Got an ADT_A01");
            // AO1 message type can be used to represent other message types

            ADT_A01 adt_01 = (ADT_A01) parser.parse(msg.encode());
            Encounter enc = dbt.addEncounter(new A01Wrap(adt_01));
            System.out.println("Added: " + enc.toString());
        } else {
            System.out.println("Other message type: " + msg.getClass().toString());
        }
    }

    /**
     * Don't want to do any normal HL7 message processing if running test profile
     */
    @Bean
    @Profile("test")
    public CommandLineRunner mainLoopTest(DBTester dbt) {
        return (args) -> {
            System.out.println("hi, just testing, doing nothing");
        };
    }

}
