package uk.ac.ucl.rits.inform.datasources.waveform;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Profile("hl7reader")
public class Hl7ParseAndSend {

    private final WaveformOperations waveformOperations;

    Hl7ParseAndSend(WaveformOperations waveformOperations) {
        this.waveformOperations = waveformOperations;
    }

    private WaveformMessage parseHl7(String messageAsStr) {
        // XXX: Need real HL7 parsing!
        Pattern srPat = Pattern.compile("/samplingRate|(.*)/");
        Pattern loPat = Pattern.compile("/locationId|(.*)/");
        Pattern stPat = Pattern.compile("/messageStartTime|(.*)/");
        Pattern idPat = Pattern.compile("/messageId|(.*)/");
        Pattern vaPat = Pattern.compile("/values|(.*)/");
        String samplingRateStr = srPat.matcher(messageAsStr).group(1);
        String locationId = loPat.matcher(messageAsStr).group(1);
        String messageStartTimeStr = stPat.matcher(messageAsStr).group(1);
        String messageId = idPat.matcher(messageAsStr).group(1);
        String valuesStr = vaPat.matcher(messageAsStr).group(1);

        WaveformMessage waveformMessage = new WaveformMessage();
        waveformMessage.setSamplingRate(Long.parseLong(samplingRateStr));
        waveformMessage.setLocationString(locationId);
        waveformMessage.setObservationTime(Instant.parse(messageStartTimeStr));
        waveformMessage.setSourceMessageId(messageId);
        String[] valuesAsString = valuesStr.split(",");
        List<Double> values = Arrays.stream(valuesAsString).map(Double::parseDouble).collect(Collectors.toList());
        waveformMessage.setNumericValues(new InterchangeValue<>(values));
        return waveformMessage;
    }

    /**
     * Parse and publish an HL7 message.
     * @param messageAsStr One HL7 message as a string
     * @throws InterruptedException .
     */
    public void parseAndSend(String messageAsStr) throws InterruptedException {
        WaveformMessage msg = parseHl7(messageAsStr);
        waveformOperations.sendMessage(msg);
    }
}
