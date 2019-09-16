package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

/**
 * Entry point class for the HL7 pipeline.
 *
 * @author Jeremy Stein
 */
@SpringBootApplication(scanBasePackages = {
        "uk.ac.ucl.rits.inform.datasinks",
        "uk.ac.ucl.rits.inform.informdb" })
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Autowired
    private AmqpTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "hl7Queue";

    /**
     * @return our Queue
     */
    @Bean
    public Queue queue() {
        Queue queue = new Queue(QUEUE_NAME, true);
        return queue;
    }

    /**
     * Added this to get Instant objects (de)serialising properly.
     * @return our message converter
     */
    @Bean
    public static Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * @param args command line args
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
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
            long startTimeMillis = System.currentTimeMillis();
            int count = 0;
            List<String> parsingErrors = new ArrayList<String>();
            while (true) {
                // read from rabbit
                EmapOperationMessage msg = (EmapOperationMessage) rabbitTemplate.receiveAndConvert(QUEUE_NAME);
                if (msg == null) {
                    int secondsSleep = 5;
                    logger.info(String.format("No more messages in RabbitMQ, retrying in %d seconds", secondsSleep));
                    try {
                        Thread.sleep(secondsSleep * 1000);
                    } catch (InterruptedException ie) {
                        // respond to a Ctrl-C, what is the right thing to do?
                        logger.warn("Sleep was interrupted");
                    }
                    continue;
                }
                msg.processMessage(dbOps);
                // This will still be needed! Turn it off for now while I get the message out.
                int processed = 0;
                // int processed = dbOps.processHl7Message(msgFromIds, idsUnid, idsLog, processed);
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
