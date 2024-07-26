package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("hl7gen")
public class Hl7Generator {
    private final Logger logger = LoggerFactory.getLogger(Hl7Generator.class);

    @Value("${test.synthetic.num_patients:30}")
    private int numPatients;

    @Value("${test.synthetic.warp_factor:1}")
    private int warpFactor;

    /**
     * You might want startDatetime and endDatetime to match the validation run start time.
     */
    @Value("${test.synthetic.start_datetime:#{null}}")
    private Instant startDatetime;

    @Value("${test.synthetic.end_datetime:#{null}}")
    private Instant endDatetime;

    /**
     * defaults that need to be computed.
     */
    @PostConstruct
    public void setComputedDefaults() {
        if (startDatetime == null) {
            startDatetime = Instant.now();
        }
    }

//    private final WaveformOperations waveformOperations;
//
//    public Hl7Generator(WaveformOperations waveformOperations) {
//        this.waveformOperations = waveformOperations;
//    }

    /**
     * Every one minute post a simulated batch of one minute's worth of data.
     * Assume 30 patients, each with a 300Hz and a 50Hz machine.
     * @throws InterruptedException dnowioinqwdnq
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void generateMessages() throws InterruptedException {
        var start = Instant.now();
        // Usually if this method runs every N seconds, you would want to generate N
        // seconds worth of data. However, for non-live tests such as validation runs,
        // you may be processing (eg.) a week's worth of data in only a few hours,
        // so it makes sense to turn up this rate to generate about the same amount of data.
        int numMillis = 60 * 1000;
        logger.info("Starting scheduled message dump (from {} for {} milliseconds)", startDatetime, numMillis);
        boolean shouldExit = false;
        for (int warpIdx = 0; warpIdx < warpFactor; warpIdx++) {
            List<String> synthMsgs = makeSyntheticWaveformMsgsAllPatients(startDatetime, numPatients, numMillis);
//            waveformOperations.sendSyntheticHl7Messages(synthMsgs);
            // XXX: send to TCP port!!!
            logger.info("Ready to send {} HL7 messages to a TCP port somewhere!", synthMsgs.size());
            startDatetime = startDatetime.plus(numMillis, ChronoUnit.MILLIS);
            if (endDatetime != null && startDatetime.isAfter(endDatetime)) {
                shouldExit = true;
                break;
            }
        }
        var end = Instant.now();
        logger.info("Full dump took {} milliseconds", start.until(end, ChronoUnit.MILLIS));
        if (shouldExit) {
            logger.info("End date {} has been reached (cur={}), EXITING", endDatetime, startDatetime);
            System.exit(0);
        }
    }


    /**
     * Make synthetic HL7 messages for a single patient and single machine, max one second per message.
     * @param samplingRate in samples per second
     * @param numMillis number of milliseconds to produce data for
     * @param locationId where the data originates from (machine/bed location)
     * @param startTime observation time of the beginning of the period that the messages are to cover
     * @param millisPerMessage max time per message (will split into multiple if needed)
     * @return all messages
     */
    private List<String> makeSyntheticWaveformMsgs(String locationId,
                                                            final long samplingRate,
                                                            final long numMillis,
                                                            final Instant startTime,
                                                            final long millisPerMessage
    ) {
        List<String> allMessages = new ArrayList<>();
        final long numSamples = numMillis * samplingRate / 1000;
        final double maxValue = 999;
        for (long overallSampleIdx = 0; overallSampleIdx < numSamples;) {
            long microsAfterStart = overallSampleIdx * 1000_000 / samplingRate;
            Instant messageStartTime = startTime.plus(microsAfterStart, ChronoUnit.MICROS);
            String messageId = String.format("%s_message%05d", locationId, overallSampleIdx);

            // XXX: make this into real HL7
            StringBuilder hl7Template = new StringBuilder();
            hl7Template.append("/samplingRate|").append(samplingRate);
            hl7Template.append("/locationId|").append(locationId);
            hl7Template.append("/messageStartTime|").append(messageStartTime);
            hl7Template.append("/messageId|").append(messageId);

            var values = new ArrayList<Double>();
            long samplesPerMessage = samplingRate * millisPerMessage / 1000;
            for (long valueIdx = 0;
                 valueIdx < samplesPerMessage && overallSampleIdx < numSamples;
                 valueIdx++, overallSampleIdx++) {
                // a sine wave between maxValue and -maxValue
                values.add(2 * maxValue * Math.sin(overallSampleIdx * 0.01) - maxValue);
            }
            String valuesAsStr = values.stream().map(Object::toString).collect(Collectors.joining(","));
            hl7Template.append("/values|").append(valuesAsStr);

            allMessages.add(hl7Template.toString());
        }
        return allMessages;
    }


    /**
     * Generate synthetic waveform data for numPatients patients to cover a period of
     * numMillis milliseconds.
     * @param startTime time to start observation period
     * @param numPatients number of patients to generate for
     * @param numMillis length of observation period to generate data for
     * @return list of HL7 messages
     * @throws InterruptedException .
     */
    public List<String> makeSyntheticWaveformMsgsAllPatients(
            Instant startTime, long numPatients, long numMillis) throws InterruptedException {
        List<String> waveformMsgs = new ArrayList<>();
        for (int p = 0; p < numPatients; p++) {
            var machine1Str = String.format("P%03d_mach1", p);
            var machine2Str = String.format("P%03d_mach2", p);
            final long millisPerMessage = 10000;
            int sizeBefore = waveformMsgs.size();
            waveformMsgs.addAll(makeSyntheticWaveformMsgs(
                    machine1Str, 50, numMillis, startTime, millisPerMessage));
            waveformMsgs.addAll(makeSyntheticWaveformMsgs(
                    machine2Str, 300, numMillis, startTime, millisPerMessage));
            int sizeAfter = waveformMsgs.size();
            logger.debug("JES: Patient {}, sending {} messages", p, sizeAfter - sizeBefore);
        }

        return waveformMsgs;

    }



}
