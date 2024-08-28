package uk.ac.ucl.rits.inform.datasources.waveform;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.ac.ucl.rits.inform.interchange.test.helpers.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig
@SpringBootTest
@ActiveProfiles("test")
public class TestWaveformCollation {
    @Autowired
    private Hl7ParseAndSend hl7ParseAndSend;
    @Autowired
    private WaveformCollator waveformCollator;

    private InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();

    Instant messageStartDatetime = Instant.parse("2022-03-04T12:11:00Z");

    @BeforeEach
    void clearMessages() {
        waveformCollator.pendingMessages.clear();
    }

    List<WaveformMessage> makeTestMessages() {
        List<WaveformMessage> uncollatedMsgs = messageFactory.getWaveformMsgs(
                "59912", "something1",
                300, 3000, 5, "UCHT03TEST",
                "", messageStartDatetime, ChronoUnit.MILLIS);
        assertEquals(600, uncollatedMsgs.size());
        return uncollatedMsgs;
    }

    private List<WaveformMessage> makeTestMessagesWithGap() {
        List<WaveformMessage> inputMessages = makeTestMessages();
        WaveformMessage removed = inputMessages.remove(300);
        return inputMessages;
    }

    static Stream<Arguments> noGapsData() {
        // We are adjusting the target number of samples config option rather than
        // the actual number of samples supplied, which may be a bit unintuitive but
        // amounts to the same thing.
        return Stream.of(
                // only just happened
                Arguments.of(3000, 10000, 1, 0),
                Arguments.of(3001, 10000, 0, 600),
                Arguments.of(2995, 10000, 1, 1),
                Arguments.of(2996, 10000, 1, 1),
                Arguments.of(1400, 10000, 2, 40),
                // comfortably in past
                Arguments.of(3000, 25000, 1, 0),
                Arguments.of(3001, 25000, 1, 0),
                Arguments.of(2995, 25000, 2, 0),
                Arguments.of(2996, 25000, 2, 0),
                Arguments.of(1400, 25000, 3, 0)
        );
    }

    // no gaps, but possible breaking into multiple messages due to sample limit
    @ParameterizedTest
    @MethodSource("noGapsData")
    void noGaps(
            int targetNumSamples,
            int nowAfterFirstMessageMillis,
            int expectedNewMessages,
            int expectedRemainingMessages) throws WaveformCollator.CollationException {

        int waitForDataLimitMillis = 15000;
        ChronoUnit assumedRounding = ChronoUnit.MILLIS;
        // GIVEN some uncollated messages (straight from HL7)
        List<WaveformMessage> inputMessages = makeTestMessages();
        waveformCollator.addMessages(inputMessages);
        Pair<String, String> keyOfInterest = new ImmutablePair<>("UCHT03TEST", "59912");
        assertEquals(1, waveformCollator.pendingMessages.size());
        assertEquals(600, waveformCollator.pendingMessages.get(keyOfInterest).size());

        // WHEN I collate the messages (which may be comfortably in the past, or have only just happened)
        Instant now = messageStartDatetime.plus(nowAfterFirstMessageMillis, assumedRounding);
        List<WaveformMessage> collatedMsgs = waveformCollator.getReadyMessages(
                now, targetNumSamples, waitForDataLimitMillis, assumedRounding);

        // THEN the messages have been combined into much fewer messages and the pending list is smaller or empty
        assertEquals(expectedNewMessages, collatedMsgs.size());
        assertEquals(expectedRemainingMessages, waveformCollator.pendingMessages.get(keyOfInterest).size());

        // getting again doesn't get any more messages
        List<WaveformMessage> collatedMsgsRepeat = waveformCollator.getReadyMessages(
                now, targetNumSamples, waitForDataLimitMillis, assumedRounding);
        assertEquals(0, collatedMsgsRepeat.size());
    }

    static Stream<Arguments> waitForMissingMessagesData() {
        return Stream.of(
                // the exact cutoff is not critical
                Arguments.of(19999, 0),
                Arguments.of(20001, 1),
                Arguments.of(24999, 1),
                Arguments.of(25001, 2)
        );
    }
    @ParameterizedTest
    @MethodSource("waitForMissingMessagesData")
    void waitForMissingMessages( int millisAfter, int expectedSize) throws WaveformCollator.CollationException {
        int waitForDataLimitMillis = 15000;
        int targetCollatedMessageSamples = 3000;
        List<WaveformMessage> inputMessages = makeTestMessagesWithGap();
        waveformCollator.addMessages(inputMessages);
        // We started with ~10 seconds of data, with a gap halfway. The default wait limit is 15 seconds after the gap,
        // which is therefore 20 seconds after the first set of data, and 25 seconds after the second set.
        Instant now = messageStartDatetime.plus(millisAfter, ChronoUnit.MILLIS);
        List<WaveformMessage> readyMessages = waveformCollator.getReadyMessages(
                now, targetCollatedMessageSamples, waitForDataLimitMillis, ChronoUnit.MILLIS);

        // The gap means that there isn't a solid chunk of waitForDataLimitMillis of data, so nothing should have been taken out.
        // Enough time has now passed that we should have given up on the data ever appearing, so collate it anyway.
        // We still can't straddle the gap within a single message, so make two messages.
        assertEquals(expectedSize, readyMessages.size());
    }

    void multipleMessagesWithMissing() {
        // both the message size limit and a gap contribute to the multiple messages being formed.
    }


    void givingUpOnMissingMessages() {
    }

    void givingUpOnMissingMessagesThenDataSubsequentlyArrives() {
    }

}
