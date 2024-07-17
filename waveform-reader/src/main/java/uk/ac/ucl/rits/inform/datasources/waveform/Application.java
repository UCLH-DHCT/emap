package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
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

    @Value("${test.synthetic.num_patients:30}")
    private int numPatients;

    @Value("${test.synthetic.warp_factor:1}")
    private int warpFactor;

    /**
     * You might want this to match the validation run start time.
     */
    @Value("${test.synthetic.start_datetime:#{null}}")
    private Instant startDatetime;

    /**
     * defaults that need to be computed.
     */
    @PostConstruct
    public void setComputedDefaults() {
        if (startDatetime == null) {
            startDatetime = Instant.now();
        }
    }

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
        var start = Instant.now();
        // Usually if this method runs every N seconds, you would want to generate N
        // seconds worth of data. However, for non-live tests such as validation runs,
        // you may be processing (eg.) a week's worth of data in only a few hours,
        // so it makes sense to turn up this rate to generate about the same amount of data.
        // (Start and end date might be even better)
        int numMillis = 60 * 1000 * warpFactor;
        logger.debug("JES: Starting scheduled message dump (from {} for {} milliseconds)", startDatetime, numMillis);
        waveformOperations.makeSyntheticWaveformMsgsAllPatients(startDatetime, numPatients, numMillis);
        startDatetime = startDatetime.plus(numMillis, ChronoUnit.MILLIS);
        var end = Instant.now();
        logger.debug("JES: Full dump took {} milliseconds", start.until(end, ChronoUnit.MILLIS));
    }


}
