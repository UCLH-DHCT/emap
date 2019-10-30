package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

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
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
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
     * The listener for processing messages and writing to Emap-Star.
     *
     * @param msg the message
     * @param channel the rabbitmq channel
     * @param tag the message tag
     * @throws IOException if rabbitmq channel has a problem
     */
    @Profile("default")
    @RabbitListener(queues = {"hl7Queue"})
    public void receiveMessage(EmapOperationMessage msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {
        dbOps.ensureVocabLoaded(); // don't want to do this every message!! put in a commandlinerunner?
        IdsEffectLogging idsEffectLogging = new IdsEffectLogging();
        Instant startTime = Instant.now();
        idsEffectLogging.setProcessingStartTime(startTime);
        idsEffectLogging.setMessageType(msg.getMessageType());
        if (msg instanceof AdtMessage) {
            idsEffectLogging.setEventReasonCode(((AdtMessage) msg).getEventReasonCode());
        }
        idsEffectLogging.setSourceId(msg.getSourceMessageId());
        try {
            String returnCode = msg.processMessage(dbOps);
            Instant doneProcessMessageTime = Instant.now();
            Duration processMessageDuration = Duration.between(startTime, doneProcessMessageTime);
            idsEffectLogging.setProcessMessageDuration(processMessageDuration.toMillis() / 1000.0);
            idsEffectLogging.setReturnStatus(returnCode);
            logger.info("Sending ACK");
            channel.basicAck(tag, false);
        } catch (EmapOperationMessageProcessingException e) {
            // All errors that allow the message to be skipped should be logged
            // using the return code from processMessage.
            idsEffectLogging.setReturnStatus(e.getReturnCode());
            idsEffectLogging.setMessage(e.getMessage());
            idsEffectLogging.setStackTrace(e);
            logger.info("Sending NACK no requeue then NOT throwing");
            channel.basicNack(tag, false, false);
        } catch (Throwable th) {
            // For anything else, at least log it before exiting.
            idsEffectLogging.setReturnStatus("Unexpected exception: " + th.toString());
            idsEffectLogging.setMessage(th.getMessage());
            idsEffectLogging.setStackTrace(th);
            logger.info("Sending NACK with requeue then throwing");
            channel.basicNack(tag, true, false);
            throw th;
        } finally {
            idsEffectLogging.setProcessingEndTime(Instant.now());
            idsEffectLoggingRepository.save(idsEffectLogging);
        }
    }

    /**
     * The main entry point.
     *
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop() {
        return (args) -> {
            // should do the ensure vocab loaded here
            logger.info("init");
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
