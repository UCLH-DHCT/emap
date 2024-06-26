package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.messaging.Publisher;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class WaveformOperations {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Publisher publisher;

    public WaveformOperations(Publisher publisher) {
        this.publisher = publisher;
    }


    private long publishMessage(Publisher publisher, long id, WaveformMessage m) throws InterruptedException {
        String mId = String.format("message%05d", id);
        m.setSourceMessageId(mId);
        //                    logger.debug("Message = {}", m.toString());
        publisher.submit(m, mId, mId, () -> {
            logger.debug("Successful ACK for message with ID {}", mId);
        });
        id++;
        return id;
    }

    /**
     * Make synthetic messages for a single patient and single machine, max one second per message.
     * @param samplingRate in samples per second
     * @param numMillis number of milliseconds to produce data for
     * @param locationId fff
     * @return all messages
     */
    private List<WaveformMessage> getWaveformMsgs(String locationId, long samplingRate, long numMillis) {
        final long millisPerMessage = 1000;

        List<WaveformMessage> allMessages = new ArrayList<>();
        long numSamples = numMillis * samplingRate / 1000;
        while (numSamples > 0) {
            WaveformMessage waveformMessage = new WaveformMessage();
            waveformMessage.setSamplingRate(samplingRate);
            waveformMessage.setLocationString(locationId);
            waveformMessage.setObservationTime(Instant.now());
            var values = new ArrayList<Double>();
            long samplesPerMessage = samplingRate * millisPerMessage / 1000;
            for (long valueIdx = 0; valueIdx < samplesPerMessage; valueIdx++) {
                values.add(Math.sin(numSamples * 0.01));
                numSamples--;
                if (numSamples == 0) {
                    break;
                }
            }
            waveformMessage.setNumericValues(new InterchangeValue<>(values));
            allMessages.add(waveformMessage);
        }
        return allMessages;
    }

    /**
     * Generate synthetic waveform data for numPatients patients.
     * @param numPatients number of patients to generate for
     * @throws InterruptedException .
     */
    public void generateSythenticPatientData(long numPatients) throws InterruptedException {
        for (int p = 0; p < numPatients; p++) {
            var machine1Str = String.format("P%03dM1", p);
            var machine2Str = String.format("P%03dM2", p);
            List<WaveformMessage> waveformMsgs1 = getWaveformMsgs(machine1Str, 50, 60 * 1000);
            List<WaveformMessage> waveformMsgs2 = getWaveformMsgs(machine2Str, 300, 60 * 1000);
            logger.debug("JES: Patient {}, sending {} + {} messages", p, waveformMsgs1.size(), waveformMsgs2.size());
            long id = 0;
            for (var m : waveformMsgs1) {
                id = publishMessage(publisher, id, m);
            }
            for (var m : waveformMsgs2) {
                id = publishMessage(publisher, id, m);
            }
        }

    }
}
