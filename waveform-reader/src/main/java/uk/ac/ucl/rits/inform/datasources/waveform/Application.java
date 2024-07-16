package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    private final WaveformOperations waveformOperations;

    public Application(WaveformOperations waveformOperations) {
        this.waveformOperations = waveformOperations;
    }

    /**
     * @param args command line args
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Every one minute post a simulated batch of one minute's worth of data.
     * Assume 30 patients, each with a 300Hz and a 50Hz machine.
     * @throws InterruptedException dnowioinqwdnq
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void mainLoop() throws InterruptedException {
        logger.debug("JES: Starting scheduled message dump");
        var start = Instant.now();
        final int numPatients = 30;
        waveformOperations.makeSyntheticWaveformMsgsAllPatients(numPatients, 60 * 1000);
        var end = Instant.now();
        logger.debug("JES: Full dump took {} milliseconds", start.until(end, ChronoUnit.MILLIS));
    }


}
