package uk.ac.ucl.rits.inform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import uk.ac.ucl.rits.inform.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.hl7.MSHWrap;
import uk.ac.ucl.rits.inform.ids.IdsOperations;

@SpringBootApplication
public class App {
    private final static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    @Profile("populate")
    public CommandLineRunner populateIDS(IdsOperations ids) {
        return (args) -> {
            HapiContext context = InitializeHapiContext();
            String hl7fileSource = args[0];
            File file = new File(hl7fileSource);
            logger.info("populating the IDS from file " + file.getAbsolutePath() + " exists = " + file.exists());
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Hl7InputStreamMessageIterator hl7iter = new Hl7InputStreamMessageIterator(is, context);
            hl7iter.setIgnoreComments(true);
            int count = 0;
            while (hl7iter.hasNext()) {
                count++;
                Message msg = hl7iter.next();
                String singleMessageText = msg.encode();
                AdtWrap adtWrap = new AdtWrap(msg);
                String triggerEvent = adtWrap.getTriggerEvent();
                String mrn = adtWrap.getMrn();
                String patientClass = adtWrap.getPatientClass();
                String patientLocation = adtWrap.getFullLocationString();
                ids.writeToIds(singleMessageText, count, triggerEvent, mrn, patientClass, patientLocation);
            }
            logger.info("Wrote " + count + " messages to IDS");
            ids.close();
            context.close();
        };
    }

    private HapiContext InitializeHapiContext() {
        HapiContext context = new DefaultHapiContext();

        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);

        // https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/HandlingMultipleVersions.html
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.7");
        context.setModelClassFactory(mcf);
        return context;
    }

    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop(InformDbOperations dbt) {
        return (args) -> {
            logger.info("Initialising HAPI...");
            long startTimeMillis = System.currentTimeMillis();
            HapiContext context = InitializeHapiContext();
            PipeParser parser = context.getPipeParser(); // getGenericParser();
            logger.info("Done initialising HAPI");
            int count = 0;
            List<String> parsingErrors = new ArrayList<String>();
            while (true) {
                int processed = dbt.processNextHl7(parser, parsingErrors);
                if (processed == -1) {
                    break;
                }
                count += processed;
                if (count % 1000 == 0) {
                    printErrorSummary(parsingErrors);
                }
            }

            long endCurrentTimeMillis = System.currentTimeMillis();
            logger.info(String.format("processed %d messages in %.0f secs", count, (endCurrentTimeMillis - startTimeMillis) / 1000.0));
            context.close();
            dbt.close();
        };
    }

    private void printErrorSummary(List<String> errors) {
        logger.debug("There are " + errors.size() + " parsing errors");
    }

    /**
     * Don't want to do any normal HL7 message processing if running test profile
     */
    @Bean
    @Profile("test")
    public CommandLineRunner mainLoopTest(InformDbOperations dbt) {
        return (args) -> {
            logger.info("Running test CommandLineRunner, which does nothing");
        };
    }

}
