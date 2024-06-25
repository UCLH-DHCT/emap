package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.messaging.Publisher;
import uk.ac.ucl.rits.inform.interchange.springconfig.EmapDataSource;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring application entry point.
 * @author Jeremy Stein
 */
@SpringBootApplication(scanBasePackages = {
        "uk.ac.ucl.rits.inform.datasources.waveform",
        "uk.ac.ucl.rits.inform.interchange",
        })
@EnableScheduling
public class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class);


    /**
     * @param args command line args
     */
    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }

    /**
     * @return the datasource enum denoting which rabbitmq queue to publish to
     */
    @Bean
    public EmapDataSource getDataSource() {
        return EmapDataSource.WAVEFORM_DATA;
    }

    private List<WaveformMessage> getWaveformMsgs(int samplingRate, int numSamples) {
        WaveformMessage waveformMessage = new WaveformMessage();
        waveformMessage.setSamplingRate(samplingRate);
        waveformMessage.setLocationString("LOCATION1");
        var values = new ArrayList<Double>();
        for (int i = 0; i < numSamples; i++) {
            values.add(Math.sin(i * 0.01));
        }
        waveformMessage.setNumericValues(new InterchangeValue<>(values));
        waveformMessage.setObservationTime(Instant.parse("2020-01-01T01:02:03Z"));
        return List.of(waveformMessage);
    }

    /**
     * Construct application with custom exception handler.
     * @param publisher to publish messages to the queue
     * @return CommandLineRunner
     */
    @Bean
    @Profile("default")
    public CommandLineRunner mainLoop(Publisher publisher) {
        return (args) -> {
            logger.debug("JES: Starting OK");
            List<WaveformMessage> waveformMsgs = getWaveformMsgs(300, 1000);
            long id = 0;
            for (int i = 0; i < 100; i++) {
                for (var m : waveformMsgs) {
                    String mId = String.format("message%05d", id);
                    m.setSourceMessageId(mId);
                    logger.debug("Message = {}", m.toString());
                    publisher.submit(m, mId, mId, () -> {
                        logger.debug("Successful ACK for message with ID {}", mId);
                    });
                }
                Thread.sleep(1000);
                id++;
            }
            logger.debug("JES: Exiting OK");
            System.exit(0);
        };
    }

}
