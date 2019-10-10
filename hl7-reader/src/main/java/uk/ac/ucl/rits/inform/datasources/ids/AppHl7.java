package uk.ac.ucl.rits.inform.datasources.ids;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;

/**
 * Entry point class for the HL7 reader application.
 *
 * @author Jeremy Stein
 */
@SpringBootApplication(scanBasePackages = {
        "uk.ac.ucl.rits.inform.datasources.ids",
        "uk.ac.ucl.rits.inform.informdb" })
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
     * @param idsOps Inform-db operations object
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop(IdsOperations idsOps) {
        return (args) -> {
            logger.info("Initialising HAPI...");
            long startTimeMillis = System.currentTimeMillis();
            HapiContext context = HL7Utils.initializeHapiContext();
            PipeParser parser = context.getPipeParser();
            logger.info("Done initialising HAPI");

            while (true) {
                try {
                    idsOps.parseAndSendNextHl7(parser);
                } catch (Exception e) {
                    // we may want to handle AmqpException specifically
                    // we need to distinguish between situations where a retry will help
                    // (eg. full queue) and where it won't.
                    logger.error("Exiting because : " + e.toString());
                    break;
                }
            }

            long endCurrentTimeMillis = System.currentTimeMillis();
            logger.info(String.format("processed messages for %.0f secs",
                    (endCurrentTimeMillis - startTimeMillis) / 1000.0));
            context.close();
            idsOps.close();
        };
    }
}
