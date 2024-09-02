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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static uk.ac.ucl.rits.inform.interchange.utils.DateTimeUtils.roundInstantToNearest;

@Component
public class WaveformCollator {
    private final Logger logger = LoggerFactory.getLogger(WaveformCollator.class);
    protected final Map<Pair<String, String>, SortedMap<Instant, WaveformMessage>> pendingMessages = new HashMap<>();

    private Pair<String, String> makeKey(WaveformMessage msg) {
        return new ImmutablePair<>(msg.getSourceLocationString(), msg.getSourceStreamId());
    }

    /**
     * Add short messages from the same patient for collating.
     * @param messagesToAdd messages to add, must all be for same patient
     * @throws CollationException if a message duplicates another message
     */
    public void addMessages(List<WaveformMessage> messagesToAdd) throws CollationException {
        /*
         * Lock the whole structure because we may add items here, and mustn't simultaneously iterate over it
         * in the reader thread.
         * Also take out lock on the per-patient data, because we may be trying to collate at the same time as
         * we're adding here.
         * This could be improved further by doing computeIfAbsent at the top outside the loop, and thus
         * avoiding holding the pendingMessages lock for too long. (Relying on all being for same patient, and
         * thus maximum one item will need adding).
         */
        synchronized (pendingMessages) {
            for (var msg : messagesToAdd) {
                // can we optimise on the basis that all messages in the list will have the same key?
                Pair<String, String> messageKey = makeKey(msg);
                SortedMap<Instant, WaveformMessage> existingMessages = pendingMessages.computeIfAbsent(
                        messageKey, k -> new TreeMap<>());
                synchronized (existingMessages) {
                    Instant observationTime = msg.getObservationTime();
                    // messages may arrive out of order, but TreeMap will keep them sorted by obs time
                    WaveformMessage existing = existingMessages.get(observationTime);
                    if (existing != null) {
                        // in future we may want to compare them and only log error if they differ
                        throw new CollationException(String.format("Would replace message with time %s: %s",
                                observationTime, existing));
                    }
                    existingMessages.put(observationTime, msg);
                }
            }
        }
    }

    /**
     * If a sufficient run of gapless messages exists for a patient, collate them
     * and delete the source messages.
     * @param nowTime for the purposes of determining how old the data is, this is the "now" time.
     *                Should be set to Instant.now() in production, but can be set differently for testing.
     * @param targetCollatedMessageSamples Wait for this many samples to exist in the queue before collating into
     *                                     a message. If we've waited more than waitForDataLimitMillis, then collate
     *                                     even if fewer samples are present. Never exceed this target.
     * @param waitForDataLimitMillis Time limit for when to relax the requirement to reach
     *                               targetCollatedMessageSamples before collating into a message.
     * @param assumedRounding what level of rounding to assume has been applied to the message timestamps, or null to
     *                        not make such an assumption.
     * @return the collated messages that are now ready for sending, may be empty if none are ready
     * @throws CollationException if any set within pendingMessages contains messages not in
     *                             fact all from the same patient+stream
     */
    public List<WaveformMessage> getReadyMessages(Instant nowTime,
                                                  int targetCollatedMessageSamples,
                                                  int waitForDataLimitMillis,
                                                  ChronoUnit assumedRounding) throws CollationException {
        List<WaveformMessage> newMessages = new ArrayList<>();
        logger.info("Pending messages: {} location+stream combos (of which {} non-empty), {} total samples",
                pendingMessages.size(),
                pendingMessages.values().stream().filter(pm -> !pm.isEmpty()).count(),
                pendingMessages.values().stream().map(Map::size).reduce(Integer::sum).orElse(0));
        List<SortedMap<Instant, WaveformMessage>> pendingMessagesSnapshot;
        synchronized (pendingMessages) {
            // Here we (briefly) iterate over pendingMessages, so take out a lock to prevent
            // undefined behaviour should we happen to be simultaneously adding items to this map.
            // (Note that items are never deleted)
            pendingMessagesSnapshot = new ArrayList<>(pendingMessages.values());
        }
        // The snapshot may become slightly out of date, but that's fine because it allows
        // more fine-grained locking to take place here.
        for (SortedMap<Instant, WaveformMessage> perPatientMap: pendingMessagesSnapshot) {
            while (true) {
                // There can be zero to multiple chunks that need turning into messages
                WaveformMessage newMsg;
                synchronized (perPatientMap) {
                    newMsg = collateContiguousData(perPatientMap, nowTime,
                            targetCollatedMessageSamples, waitForDataLimitMillis, assumedRounding);
                }
                if (newMsg == null) {
                    break;
                } else {
                    newMessages.add(newMsg);
                }
            }
        }
        return newMessages;
    }

    /**
     * Given a sorted map of messages (all for same patient+stream), squash as much as possible
     * into a single message, respecting the target number of samples. If a time gap is detected
     * in the sequence of messages, stop. Ie. do not straddle the gap within the same message.
     * Remove messages from the structure which were used as source data for the collated message.
     * Returns only one message, must be called repeatedly to see if more collating can be done.
     * @param perPatientMap sorted messages to collate, will have source items deleted from it
     * @param nowTime see {@link #getReadyMessages}
     * @param targetCollatedMessageSamples see {@link #getReadyMessages}
     * @param waitForDataLimitMillis see {@link #getReadyMessages}
     * @param assumedRounding see {@link #getReadyMessages}
     * @return the collated message, or null if the messages cannot be collated
     * @throws CollationException if perPatientMap messages are not in fact all from the same patient+stream
     */

    private WaveformMessage collateContiguousData(SortedMap<Instant, WaveformMessage> perPatientMap,
                                                  Instant nowTime,
                                                  int targetCollatedMessageSamples,
                                                  int waitForDataLimitMillis,
                                                  ChronoUnit assumedRounding) throws CollationException {
        if (perPatientMap.isEmpty()) {
            // maps are not removed after being emptied, so this situation can exist and is harmless
            return null;
        }
        WaveformMessage firstMsg = perPatientMap.get(perPatientMap.firstKey());
        Pair<String, String> firstKey = makeKey(firstMsg);

        int sizeBefore = perPatientMap.size();
        long sampleCount = 0;
        Instant expectedNextDatetime = null;
        // existing values are not necessarily in mutable lists so use a new ArrayList
        List<Double> newNumericValues = new ArrayList<>();
        Iterator<Map.Entry<Instant, WaveformMessage>> perPatientMapIter = perPatientMap.entrySet().iterator();
        int messagesToCollate = 0;
        while (perPatientMapIter.hasNext()) {
            Map.Entry<Instant, WaveformMessage> entry = perPatientMapIter.next();
            WaveformMessage msg = entry.getValue();
            Pair<String, String> thisKey = makeKey(msg);
            if (!thisKey.equals(firstKey)) {
                throw new CollationException(String.format("Key Mismatch: %s vs %s", firstKey, thisKey));
            }

            sampleCount += msg.getNumericValues().get().size();
            if (sampleCount > targetCollatedMessageSamples) {
                logger.info("Reached sample target ({} > {}), collated message span: {} -> {}",
                        sampleCount, targetCollatedMessageSamples,
                        firstMsg.getObservationTime(), msg.getObservationTime());
                break;
            }

            if (expectedNextDatetime != null) {
                Instant gapUpperBound = checkGap(msg, expectedNextDatetime, assumedRounding);
                if (gapUpperBound != null) {
                    logger.info("Key {}, collated message span: {} -> {} ({} milliseconds, {} samples)",
                            makeKey(msg),
                            firstMsg.getObservationTime(), msg.getObservationTime(),
                            firstMsg.getObservationTime().until(msg.getObservationTime(), ChronoUnit.MILLIS),
                            sampleCount);
                    // Found a gap, stop here. Decide later whether data is old enough to make a message anyway.
                    break;
                }
            }
            expectedNextDatetime = msg.getExpectedNextObservationDatetime();

            // don't modify yet, because we don't yet know if we will reach criteria to collate (num samples, time passed)
            messagesToCollate++;
        }

        // If we have not reached the message size threshold, whether because there aren't enough samples
        // or we reached a gap, then do not collate yet; give the data a bit more time to appear.
        // UNLESS enough time has already passed, then prioritise timeliness and collate anyway.
        // (If the data does subsequently arrive, then it'll likely be "collated" into a message by itself)
        // In other words, if not enough samples and not enough time has passed, then do not collate.
        if (sampleCount < targetCollatedMessageSamples
                && expectedNextDatetime.until(nowTime, ChronoUnit.MILLIS) <= waitForDataLimitMillis) {
            return null;
        }

        Iterator<Map.Entry<Instant, WaveformMessage>> secondPassIter = perPatientMap.entrySet().iterator();
        for (int i = 0; i < messagesToCollate; i++) {
            Map.Entry<Instant, WaveformMessage> entry = secondPassIter.next();
            WaveformMessage msg = entry.getValue();
            newNumericValues.addAll(msg.getNumericValues().get());
            // Remove all messages from the map that are used as source data, even the first one.
            // The underlying message object of the first element will still exist.
            secondPassIter.remove();
        }
        firstMsg.setNumericValues(new InterchangeValue<>(newNumericValues));
        int sizeAfter = perPatientMap.size();
        logger.info("Key {}, Collated {} messages into one, ({} data points)",
                firstKey, sizeBefore - sizeAfter, sampleCount);
        return firstMsg;
    }

    private Instant checkGap(WaveformMessage msg, Instant expectedNextDatetime, ChronoUnit assumedRounding) {
        // gap between this message and previous message
        long samplePeriodMicros = 1_000_000L / msg.getSamplingRate();
        Instant expectedNextDatetimeRounded = roundInstantToNearest(expectedNextDatetime, assumedRounding);
        long gapSizeMicros = expectedNextDatetime.until(msg.getObservationTime(), ChronoUnit.MICROS);
        long gapSizeToRoundedMicros = expectedNextDatetimeRounded.until(msg.getObservationTime(), ChronoUnit.MICROS);
        /* The timestamps in the messages will be rounded. Currently assuming that it's to the nearest
         * millisecond, but for all I know it could be rounding down.
         * Take 3.33 ms, a common sampling period (300Hz): rounding to the nearest ms can produce a large
         * relative error. The error will also be inconsistent: when the stars align it might be zero.
         * So try for now: To be counted as abutting, the actual timestamp has to be close to *either* the
         * rounded or unrounded expected timestamp, thus allowing the error margin to be set much stricter,
         * since it's now only accounting for non-rounding sources of error (whatever they might be).
         * Ah no, just allow it to be one rounding unit off :/
         */
        logger.trace("expectedNextDatetime {}, expectedNextDatetimeRounded {}, msg.getObservationTime() {}",
                expectedNextDatetime, expectedNextDatetimeRounded, msg.getObservationTime());
        long allowedGapMicros = 1000;
    //                double allowedGapMicros = samplePeriodMicros * 0.05;
        if (Math.abs(gapSizeMicros) > allowedGapMicros && Math.abs(gapSizeToRoundedMicros) > allowedGapMicros) {
            logger.info("Key {}, Gap too big ({} microsecs vs rounded, {} vs unrounded)",
                    makeKey(msg), gapSizeToRoundedMicros, gapSizeMicros);
            return msg.getObservationTime();
        }

        return null;
    }

    class CollationException extends Throwable {
        CollationException(String format) {
        }
    }
}