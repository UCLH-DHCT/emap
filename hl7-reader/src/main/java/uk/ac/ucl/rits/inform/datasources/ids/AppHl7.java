package uk.ac.ucl.rits.inform.datasources.ids;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.ReachedEndException;
import uk.ac.ucl.rits.inform.interchange.messaging.Publisher;

/**
 * Entry point class for the HL7 reader application.
 *
 * @author Jeremy Stein
 */
@SpringBootApplication(scanBasePackages = {
        "uk.ac.ucl.rits.inform.datasources.ids",
        "uk.ac.ucl.rits.inform.interchange"})
public class AppHl7 {
    private static final Logger logger = LoggerFactory.getLogger(AppHl7.class);

    /**
     * Launch spring.
     * @param args command line args
     */
    public static void main(String[] args) {
        SpringApplication.run(AppHl7.class, args);
    }

    /**
     * The entry point for processing HL7 messages and writing interchange messages to the queue.
     *
     * @param publisher     the local AMQP handling class
     * @param idsOps        Inform-db operations object
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop(Publisher publisher, IdsOperations idsOps) {
        return (args) -> {
            logger.info("Initialising HAPI...");
            long startTimeMillis = System.currentTimeMillis();
            HapiContext context = HL7Utils.initializeHapiContext();
            PipeParser parser = context.getPipeParser();
            logger.info("Done initialising HAPI");

            int exitCode = 1;
            while (true) {
                try {
                    idsOps.parseAndSendNextHl7(publisher, parser);
                } catch (ReachedEndException ree) {
                    // last message has been processed, so stop
                    exitCode = 0;
                    break;
                } catch (Exception e) {
                    // we may want to handle AmqpException specifically
                    // we need to distinguish between situations where a retry will help
                    // (eg. full queue) and where it won't.
                    StringWriter st = new StringWriter();
                    e.printStackTrace(new PrintWriter(st));
                    logger.error("Exiting because : " + st.toString());
                    break;
                }
            }

            long endCurrentTimeMillis = System.currentTimeMillis();
            logger.info(String.format("processed messages for %.0f secs, exiting with code %d",
                    (endCurrentTimeMillis - startTimeMillis) / 1000.0, exitCode));
            context.close();
            idsOps.close();
            // Make sure all threads exit - if running in open-ended mode, ie. no IDS endUnid
            // to stop at, then the only way to get here is because something
            // abnormal has happened
            System.exit(exitCode);
        };
    }
}
