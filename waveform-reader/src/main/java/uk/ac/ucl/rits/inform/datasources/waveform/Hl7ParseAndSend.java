package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class Hl7ParseAndSend {
    private final Logger logger = LoggerFactory.getLogger(Hl7ParseAndSend.class);

    private final WaveformOperations waveformOperations;

    Hl7ParseAndSend(WaveformOperations waveformOperations) {
        this.waveformOperations = waveformOperations;
    }

    private String getReMatch(String inputText, String field) {
        Pattern pat = Pattern.compile("/" + field + "/(.*?)/");
        Matcher mat = pat.matcher(inputText);
        mat.find();
        return mat.group(1);
    }

    private WaveformMessage parseHl7(String messageAsStr) {
        // XXX: Need real HL7 parsing!
        logger.info("Parsing message of size {}", messageAsStr.length());
        String samplingRateStr = getReMatch(messageAsStr, "samplingRate");
        String locationId = getReMatch(messageAsStr, "locationId");
        String messageStartTimeStr = getReMatch(messageAsStr, "messageStartTime");
        String messageId = getReMatch(messageAsStr, "messageId");
        String valuesStr = getReMatch(messageAsStr, "values");

        WaveformMessage waveformMessage = new WaveformMessage();
        waveformMessage.setSamplingRate(Long.parseLong(samplingRateStr));
        waveformMessage.setSourceLocationString(locationId);
        // XXX: need to perform location mapping here and set the mapped location
        waveformMessage.setObservationTime(Instant.parse(messageStartTimeStr));
        waveformMessage.setSourceMessageId(messageId);
        String[] valuesArray = valuesStr.split(",");
        logger.trace("valuesArray = {} (length = {})", valuesArray, valuesArray.length);
        List<Double> values = Arrays.stream(valuesArray).map(Double::parseDouble).collect(Collectors.toList());
        waveformMessage.setNumericValues(new InterchangeValue<>(values));
        logger.debug("waveform message contains {} numerical values", values.size());
        logger.trace("output interchange waveform message = {}", waveformMessage);
        return waveformMessage;
    }

    /**
     * Parse and publish an HL7 message.
     * @param messageAsStr One HL7 message as a string
     * @throws InterruptedException if publisher send is interrupted
     */
    public void parseAndSend(String messageAsStr) throws InterruptedException {
        WaveformMessage msg = parseHl7(messageAsStr);
        waveformOperations.sendMessage(msg);
    }
}
