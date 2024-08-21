package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.waveform.hl7parse.Hl7Message;
import uk.ac.ucl.rits.inform.datasources.waveform.hl7parse.Hl7ParseException;
import uk.ac.ucl.rits.inform.datasources.waveform.hl7parse.Hl7Segment;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class Hl7ParseAndSend {
    private final Logger logger = LoggerFactory.getLogger(Hl7ParseAndSend.class);

    private final WaveformOperations waveformOperations;
    private final WaveformCollator waveformCollator;

    Hl7ParseAndSend(WaveformOperations waveformOperations,
                    WaveformCollator waveformCollator) {
        this.waveformOperations = waveformOperations;
        this.waveformCollator = waveformCollator;
    }

    // XXX: this will have to be returning some kind of incomplete message form because
    // we will be merging messages so they're big enough to prevent performance/storage
    // problems in the core proc/DB
    List<WaveformMessage> parseHl7(String messageAsStr) throws Hl7ParseException {
        List<WaveformMessage> allWaveformMessages = new ArrayList<>();
        logger.info("Parsing message of size {}", messageAsStr.length());
        Instant start = Instant.now();
        Hl7Message message = new Hl7Message(messageAsStr);
        String messageIdBase = message.getField("MSH", 10);
        String pv1LocationId = message.getField("PV1", 3);
        String messageType = message.getField("MSH", 9);
        if (!messageType.equals("ORU^R01")) {
            throw new Hl7ParseException("Was expecting ORU^R01, got " + messageType);
        }
        List<Hl7Segment> allObr = message.getSegments("OBR");
        int obrI = 0;
        for (var obr: allObr) {
            obrI++;
            String locationId = obr.getField(10);
            List<Hl7Segment> allObx = obr.getChildSegments("OBX");
            int obxI = 0;
            for (var obx: allObx) {
                obxI++;
                String obsDatetimeStr = obx.getField(14);

                if (!pv1LocationId.equals(locationId)) {
                    throw new Hl7ParseException("Unexpected location " + locationId + "|" + pv1LocationId);
                }

                logger.info("Parsing datetime {}", obsDatetimeStr);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSZZ");
                TemporalAccessor ta = formatter.parse(obsDatetimeStr);
                Instant obsDatetime = Instant.from(ta);

                String streamId = obx.getField(3);
                String allPointsStr = obx.getField(5);
                if (allPointsStr.contains("~")) {
                    throw new Hl7ParseException("must only be 1 repeat in OBX-5");
                }
                List<Double> points = Arrays.stream(allPointsStr.split("\\^")).map(Double::parseDouble).toList();

                // XXX: Sampling rate is not in the message.
                // Will be fixed by implementing issue #45.
                long samplingRate;
                if (streamId.equals("59912")) {
                    samplingRate = 50L;
                } else {
                    samplingRate = 300L;
                }

                String messageIdSpecific = String.format("%s_%d_%d", messageIdBase, obrI, obxI);
                logger.debug("location {}, time {}, messageId {}, value count = {}",
                        locationId, obsDatetime, messageIdSpecific, points.size());
                WaveformMessage waveformMessage = waveformMessageFromValues(
                        samplingRate, locationId, obsDatetime, messageIdSpecific, streamId, points);

                allWaveformMessages.add(waveformMessage);
            }
        }

        Instant afterParse2 = Instant.now();
        logger.info("Timing: message length {}, parse {} ms",
                messageAsStr.length(),
                start.until(afterParse2, ChronoUnit.MILLIS));
        return allWaveformMessages;
    }

    private WaveformMessage waveformMessageFromValues(
            Long samplingRate, String locationId, Instant messageStartTime, String messageId,
            String sourceStreamId, List<Double> arrayValues) {
        WaveformMessage waveformMessage = new WaveformMessage();
        waveformMessage.setSamplingRate(samplingRate);
        waveformMessage.setSourceLocationString(locationId);
        // XXX: need to perform location mapping here and set the mapped location
        // XXX: ditto stream ID mapping
        waveformMessage.setObservationTime(messageStartTime);
        waveformMessage.setSourceMessageId(messageId);
        waveformMessage.setSourceStreamId(sourceStreamId);
        waveformMessage.setNumericValues(new InterchangeValue<>(arrayValues));
        logger.trace("output interchange waveform message = {}", waveformMessage);
        return waveformMessage;
    }

    /**
     * Parse and publish an HL7 message.
     * @param messageAsStr One HL7 message as a string
     * @throws InterruptedException if publisher send is interrupted
     * @throws Hl7ParseException .
     */
    public void parseAndSend(String messageAsStr) throws InterruptedException, Hl7ParseException {
        List<WaveformMessage> msgs = parseHl7(messageAsStr);

        logger.info("HL7 message generated {} Waveform messages, sending for collation", msgs.size());
        waveformCollator.addMessages(msgs);
    }

    /**
     * See what abutting messages are available for collation and sending.
     * @throws InterruptedException .
     */
    @Scheduled(fixedDelay = 10 * 1000)
    public void collateAndSend() throws InterruptedException {
        List<WaveformMessage> msgs = waveformCollator.getReadyMessages();
        logger.info("{} Waveform messages ready for sending", msgs.size());
        for (var m: msgs) {
            // consider sending to publisher in batches?
            waveformOperations.sendMessage(m);
        }
    }
}
