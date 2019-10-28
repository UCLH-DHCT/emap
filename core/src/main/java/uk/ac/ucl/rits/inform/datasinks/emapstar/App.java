package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
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

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.IdsEffectLogging;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.IdsEffectLoggingRepository;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.springconfig.EmapDataSource;

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

    @Autowired
    private IdsEffectLoggingRepository idsEffectLoggingRepository;

    private static final String QUEUE_NAME = EmapDataSource.HL7_QUEUE.getQueueName();

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
            while (true) {
                IdsEffectLogging idsEffectLogging = new IdsEffectLogging();
                Instant startTime = Instant.now();
                idsEffectLogging.setProcessingStartTime(startTime);
                // read from rabbit
                EmapOperationMessage msg;
                Instant doneQueueReadTime;
                try {
                    msg = (EmapOperationMessage) rabbitTemplate.receiveAndConvert(QUEUE_NAME);
                    doneQueueReadTime = Instant.now();
                    Duration queueReadDuration = Duration.between(startTime, doneQueueReadTime);
                    idsEffectLogging.setQueueReadDuration(queueReadDuration.toMillis() / 1000.0);
                } catch (AmqpException e) {
                    int secondsSleep = 5;
                    logger.warn(String.format("Read from RabbitMQ failed with exception %s, retrying in %d seconds", e.toString(), secondsSleep));
                    try {
                        Thread.sleep(secondsSleep * 1000);
                    } catch (InterruptedException ie) {
                        // respond to a Ctrl-C
                        logger.warn("Sleep was interrupted, exiting");
                        break;
                    }
                    continue;
                }
                if (msg == null) {
                    int secondsSleep = 5;
                    logger.info(String.format("No more messages in RabbitMQ, retrying in %d seconds", secondsSleep));
                    try {
                        Thread.sleep(secondsSleep * 1000);
                    } catch (InterruptedException ie) {
                        // respond to a Ctrl-C
                        logger.warn("Sleep was interrupted, exiting");
                        break;
                    }
                    continue;
                }
                idsEffectLogging.setMessageType(msg.getMessageType());
                idsEffectLogging.setSourceId(msg.getSourceMessageId());
                try {
                    String returnCode = msg.processMessage(dbOps);
                    Instant doneProcessMessageTime = Instant.now();
                    Duration processMessageDuration = Duration.between(doneQueueReadTime, doneProcessMessageTime);
                    idsEffectLogging.setProcessMessageDuration(processMessageDuration.toMillis() / 1000.0);
                    idsEffectLogging.setReturnStatus(returnCode);
                } catch (EmapOperationMessageProcessingException e) {
                    // All errors that allow the message to be skipped should be logged
                    // using the return code from processMessage.
                    idsEffectLogging.setReturnStatus(e.getReturnCode());
                    idsEffectLogging.setMessage(e.getMessage());
                    idsEffectLogging.setStackTrace(e);
                } catch (Throwable th) {
                    // For anything else, at least log it before exiting.
                    idsEffectLogging.setReturnStatus("Unexpected exception: " + th.toString());
                    idsEffectLogging.setMessage(th.getMessage());
                    idsEffectLogging.setStackTrace(th);
                    throw th;
                } finally {
                    idsEffectLogging.setProcessingEndTime(Instant.now());
                    idsEffectLoggingRepository.save(idsEffectLogging);
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
