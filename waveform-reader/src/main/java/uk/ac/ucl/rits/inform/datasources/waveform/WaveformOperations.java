package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.messaging.Publisher;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class WaveformOperations {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Publisher publisher;

    public WaveformOperations(Publisher publisher) {
        this.publisher = publisher;
    }


    private void publishMessage(Publisher publisher, String messageId, WaveformMessage m) throws InterruptedException {
        //                    logger.debug("Message = {}", m.toString());
        publisher.submit(m, messageId, messageId, () -> {
            logger.debug("Successful ACK for message with ID {}", messageId);
        });
    }

    /**
     * Make synthetic messages for a single patient and single machine, max one second per message.
     * @param samplingRate in samples per second
     * @param numMillis number of milliseconds to produce data for
     * @param locationId where the data originates from (machine/bed location)
     * @param startTime observation time of the beginning of the period that the messages are to cover
     * @param millisPerMessage max time per message (will split into multiple if needed)
     * @return all messages
     */
    private List<WaveformMessage> makeSyntheticWaveformMsgs(String locationId,
                                                            final long samplingRate,
                                                            final long numMillis,
                                                            final Instant startTime,
                                                            final long millisPerMessage
    ) {
        List<WaveformMessage> allMessages = new ArrayList<>();
        final long numSamples = numMillis * samplingRate / 1000;
        final double maxValue = 999;
        for (long overallSampleIdx = 0; overallSampleIdx < numSamples;) {
            long microsAfterStart = overallSampleIdx * 1000_000 / samplingRate;
            Instant messageStartTime = startTime.plus(microsAfterStart, ChronoUnit.MICROS);
            String messageId = String.format("%s_message%05d", locationId, overallSampleIdx);
            WaveformMessage waveformMessage = new WaveformMessage();
            waveformMessage.setSamplingRate(samplingRate);
            waveformMessage.setLocationString(locationId);
            waveformMessage.setObservationTime(messageStartTime);
            waveformMessage.setSourceMessageId(messageId);
            var values = new ArrayList<Double>();
            long samplesPerMessage = samplingRate * millisPerMessage / 1000;
            for (long valueIdx = 0;
                 valueIdx < samplesPerMessage && overallSampleIdx < numSamples;
                 valueIdx++, overallSampleIdx++) {
                // a sine wave between maxValue and -maxValue
                values.add(2 * maxValue * Math.sin(overallSampleIdx * 0.01) - maxValue);
            }
            waveformMessage.setNumericValues(new InterchangeValue<>(values));
            allMessages.add(waveformMessage);
        }
        return allMessages;
    }

    /**
     * Generate synthetic waveform data for numPatients patients to cover a period of
     * numMillis milliseconds.
     * @param startTime time to start observation period
     * @param numPatients number of patients to generate for
     * @param numMillis length of observation period to generate data for
     * @throws InterruptedException .
     */
    public void makeSyntheticWaveformMsgsAllPatients(
            Instant startTime, long numPatients, long numMillis) throws InterruptedException {
        for (int p = 0; p < numPatients; p++) {
            var machine1Str = String.format("P%03d_mach1", p);
            var machine2Str = String.format("P%03d_mach2", p);
            final long millisPerMessage = 10000;
            List<WaveformMessage> waveformMsgs = new ArrayList<>();
            waveformMsgs.addAll(makeSyntheticWaveformMsgs(
                    machine1Str, 50, numMillis, startTime, millisPerMessage));
            waveformMsgs.addAll(makeSyntheticWaveformMsgs(
                    machine2Str, 300, numMillis, startTime, millisPerMessage));
            logger.debug("JES: Patient {}, sending {} messages", p, waveformMsgs.size());
            for (var m : waveformMsgs) {
                publishMessage(publisher, m.getSourceMessageId(), m);
            }
        }

    }
}
