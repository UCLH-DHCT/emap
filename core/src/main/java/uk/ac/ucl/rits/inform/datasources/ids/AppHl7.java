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

    public static void main(String[] args) {
        logger.info("STARTING HL7 READER");
        SpringApplication.run(AppHl7.class, args);
    }

    /**
     * The main entry point for processing HL7 messages and writing to Inform-db.
     *
     * @param dbOps Inform-db operations object
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
            int count = 0;
            while (true) {
                int processed = idsOps.parseAndSendNextHl7(parser);
                if (processed == -1) {
                    break;
                }
                count += processed;
            }

            long endCurrentTimeMillis = System.currentTimeMillis();
            logger.info(String.format("processed %d messages in %.0f secs", count,
                    (endCurrentTimeMillis - startTimeMillis) / 1000.0));
            context.close();
            idsOps.close();
        };
    }
}
