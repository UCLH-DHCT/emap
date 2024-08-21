package uk.ac.ucl.rits.inform.datasources.waveform;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class WaveformCollator {
    private final Logger logger = LoggerFactory.getLogger(WaveformCollator.class);
    private final Map<Pair<String, String>, SortedMap<Instant, WaveformMessage>> pendingMessages = new HashMap<>();

    private Pair<String, String> makeKey(WaveformMessage msg) {
        return new ImmutablePair<>(msg.getSourceLocationString(), msg.getSourceStreamId());
    }

    /**
     * Add short messages from the same patient for collating.
     * @param messagesToAdd messages to add, must all be for same patient
     */
    public void addMessages(List<WaveformMessage> messagesToAdd) {
        /*
         * Will probably need per-patient locks otherwise the threads won't be able to do much.
         * But that will complicate iterating.
         */
        synchronized (this) {
            for (var msg : messagesToAdd) {
                // can we optimise on the basis that all messages in the list will have the same key?
                Pair<String, String> messageKey = makeKey(msg);
                SortedMap<Instant, WaveformMessage> existingMessages = pendingMessages.computeIfAbsent(
                        messageKey, k -> new TreeMap<>());
                Instant observationTime = msg.getObservationTime();
                // messages may arrive out of order, but TreeMap will keep them sorted by obs time
                existingMessages.put(observationTime, msg);
            }
        }
    }

    /**
     * If a sufficient run of gapless messages exists for a patient, collate them
     * and delete the source messages.
     * @return the collated messages that are now ready for sending
     */
    public List<WaveformMessage> getReadyMessages() {
        final long minimumSpanMillis = 10000;
        List<WaveformMessage> newMessages = new ArrayList<>();
        synchronized (this) {
            for (SortedMap<Instant, WaveformMessage> perPatientMap: pendingMessages.values()) {
                Instant earliestDatetime = perPatientMap.firstKey();
                Instant latestDatetime = perPatientMap.lastKey();
                long spanMillis = earliestDatetime.until(latestDatetime, ChronoUnit.MILLIS);
                if (spanMillis < minimumSpanMillis) {
                    continue;
                }
                // even if we've achieved a certain span, there may still be gaps!
                Instant noGapsUntil = checkForGaps(perPatientMap.values());
                WaveformMessage newMsg;
                if (noGapsUntil == null) {
                    newMsg = collate(perPatientMap, earliestDatetime.plus(minimumSpanMillis, ChronoUnit.MILLIS));
                } else {
                    newMsg = collate(perPatientMap, noGapsUntil);
                }
                newMessages.add(newMsg);
            }
        }
        return newMessages;
    }

    private WaveformMessage collate(SortedMap<Instant, WaveformMessage> perPatientMap,
                                    Instant upperLimit) {
        WaveformMessage firstMsg = perPatientMap.get(perPatientMap.firstKey());

        int sizeBefore = perPatientMap.size();
        Iterator<Map.Entry<Instant, WaveformMessage>> entryIter = perPatientMap.entrySet().iterator();
        long totalPointsSummed = firstMsg.getNumericValues().get().size();
        // existing values are not necessarily mutable so use a new ArrayList
        List<Double> newNumericValues = new ArrayList<>();
        while (entryIter.hasNext()) {
            Map.Entry<Instant, WaveformMessage> entry = entryIter.next();
            Instant k = entry.getKey();
            WaveformMessage msg = entry.getValue();
            newNumericValues.addAll(msg.getNumericValues().get());
            if (k.compareTo(upperLimit) >= 0) {
                logger.info("Reached upper limit {}", upperLimit);
                break;
            }
            totalPointsSummed += msg.getNumericValues().get().size();
            // Remove all Map entries up to the upper limit, even the first one.
            // We still want the underlying object of the first element to exist.
            entryIter.remove();
        }
        firstMsg.setNumericValues(new InterchangeValue<>(newNumericValues));
        int sizeAfter = perPatientMap.size();
        if (totalPointsSummed != firstMsg.getNumericValues().get().size()) {
            throw new RuntimeException(String.format("Oh dear, should be %d entries, got %d",
                    totalPointsSummed, firstMsg.getNumericValues().get().size()));
        }
        logger.info("Collated {} messages into one, ({} data points)", sizeBefore - sizeAfter, totalPointsSummed);
        return firstMsg;
    }

    /**
     * @param allMessages list of sorted messages to check for gaps in
     * @return null if no gaps, or if gaps the (exclusive) datetime limit before which there are no gaps
     */
    private Instant checkForGaps(Collection<WaveformMessage> allMessages) {
        Instant expectedNextDatetime = null;
        for (WaveformMessage msg: allMessages) {
            if (expectedNextDatetime != null) {
                // gap between this message and previous message
                long samplePeriodMicros = 1_000_000L / msg.getSamplingRate();
                long gapSizeMicros = expectedNextDatetime.until(msg.getObservationTime(), ChronoUnit.MICROS);
                // some rounding error is likely, but let's say for now that anything
                // less than 10% of a sample period is a good enough score to just join them together
                if (Math.abs(gapSizeMicros) > samplePeriodMicros / 10) {
                    logger.info("Gap too big ({} microsecs), skipping for now", gapSizeMicros);
                    return msg.getObservationTime();
                }
            }
            expectedNextDatetime = msg.getExpectedNextObservationDatetime();
        }
        return null;
    }
}
