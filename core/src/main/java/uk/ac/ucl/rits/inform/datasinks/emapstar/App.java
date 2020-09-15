package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.IdsEffectLogging;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.IdsEffectLoggingRepository;
import uk.ac.ucl.rits.inform.interchange.OldAdtMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

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
    private InformDbOperations dbOps;

    @Autowired
    private IdsEffectLoggingRepository idsEffectLoggingRepository;

    /**
     * Added this to get Instant objects (de)serialising properly.
     * @return our message converter
     */
    @Bean
    public static MessageConverter jsonReaderMessageConverter() {
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
     * For ensuring vocab gets loaded before any processing happens.
     */
    private CountDownLatch vocabLoaded = new CountDownLatch(1);

    /**
     * The listener for processing messages and writing to Emap-Star. The ordering
     * of queue names in the `queues` parameter actually matters - we want HL7
     * messages to be processed in preference to caboodle messages.
     *
     * @param msg     the message
     * @param channel the rabbitmq channel
     * @param tag     the message tag
     * @throws IOException if rabbitmq channel has a problem
     */
    @Profile("default")
    @RabbitListener(queues = {"hl7Queue", "caboodleQueue"})
    public void receiveMessage(EmapOperationMessage msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {
        try {
            vocabLoaded.await();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        IdsEffectLogging idsEffectLogging = new IdsEffectLogging();
        Instant startTime = Instant.now();
        idsEffectLogging.setProcessingStartTime(startTime);
        idsEffectLogging.setMessageType(msg.getMessageType());
        if (msg instanceof OldAdtMessage) {
            idsEffectLogging.setEventReasonCode(((OldAdtMessage) msg).getEventReasonCode());
        }
        idsEffectLogging.setSourceId(msg.getSourceMessageId());
        try {
            String returnCode = msg.processMessage(dbOps);
            Instant doneProcessMessageTime = Instant.now();
            Duration processMessageDuration = Duration.between(startTime, doneProcessMessageTime);
            idsEffectLogging.setProcessMessageDuration(processMessageDuration.toMillis() / 1000.0);
            idsEffectLogging.setReturnStatus(returnCode);
            logger.info("Sending ACK for " + msg.getSourceMessageId());
            channel.basicAck(tag, false);
        } catch (EmapOperationMessageProcessingException e) {
            // All errors that allow the message to be skipped should be logged
            // using the return code from processMessage.
            idsEffectLogging.setReturnStatus(e.getReturnCode());
            idsEffectLogging.setMessage(e.getMessage());
            idsEffectLogging.setStackTrace(e);
            logger.info("Sending NACK no requeue then NOT throwing for " + msg.getSourceMessageId());
            channel.basicNack(tag, false, false);
        } catch (Throwable th) {
            // For anything else, at least log it before exiting.
            idsEffectLogging.setReturnStatus("Unexpected exception: " + th.toString());
            idsEffectLogging.setMessage(th.getMessage());
            idsEffectLogging.setStackTrace(th);
            logger.info("Sending NACK no requeue then throwing for " + msg.getSourceMessageId());
            channel.basicNack(tag, false, false);
            throw th;
        } finally {
            idsEffectLogging.setProcessingEndTime(Instant.now());
            idsEffectLoggingRepository.save(idsEffectLogging);
        }
    }

    /**
     * Initialise vocab (attributes). Perform early in the init process.
     *
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("default")
    public CommandLineRunner loadVocab() {
        return (args) -> {
//            dbOps.ensureVocabLoaded();
            vocabLoaded.countDown();
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
//            dbOps.ensureVocabLoaded();
            logger.info("Running test CommandLineRunner, to ensure the vocab is loaded.");
        };
    }

}
