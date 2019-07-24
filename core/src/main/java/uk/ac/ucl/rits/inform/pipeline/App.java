package uk.ac.ucl.rits.inform.pipeline;

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

import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import uk.ac.ucl.rits.inform.pipeline.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.pipeline.hl7.HL7Utils;
import uk.ac.ucl.rits.inform.pipeline.ids.IdsOperations;

/**
 * Entry point class for the HL7 pipeline.
 *
 * @author Jeremy Stein
 */
@SpringBootApplication
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * @param args command line args
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    /**
     * Entry point for populating a test IDS from a file specified on the command
     * line.
     *
     * @param ids IDS operations objects
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("populate")
    public CommandLineRunner populateIDS(IdsOperations ids) {
        return (args) -> {
            HapiContext context = HL7Utils.initializeHapiContext();
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

    /**
     * The main entry point for processing HL7 messages and writing to Inform-db.
     *
     * @param dbOps Inform-db operations object
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop(InformDbOperations dbOps) {
        return (args) -> {
            dbOps.ensureVocabLoaded();
            logger.info("Initialising HAPI...");
            long startTimeMillis = System.currentTimeMillis();
            HapiContext context = HL7Utils.initializeHapiContext();
            PipeParser parser = context.getPipeParser();
            logger.info("Done initialising HAPI");
            int count = 0;
            List<String> parsingErrors = new ArrayList<String>();
            while (true) {
                int processed = dbOps.processNextHl7(parser, parsingErrors);
                if (processed == -1) {
                    break;
                }
                count += processed;
                if (count % 1000 == 0) {
                    logger.debug("There are " + parsingErrors.size() + " parsing errors");
                }
            }

            long endCurrentTimeMillis = System.currentTimeMillis();
            logger.info(String.format("processed %d messages in %.0f secs", count,
                    (endCurrentTimeMillis - startTimeMillis) / 1000.0));
            context.close();
            dbOps.close();
        };
    }

    /**
     * Don't want to do any normal HL7 message processing if running test profile.
     *
     * @param dbOps Database operations functions.
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("test")
    public CommandLineRunner mainLoopTest(InformDbOperations dbOps) {
        return (args) -> {
            dbOps.ensureVocabLoaded();
            logger.info("Running test CommandLineRunner, to ensure the vocab is loaded.");
        };
    }

}
