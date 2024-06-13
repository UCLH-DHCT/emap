package uk.ac.ucl.rits.inform.interchange.visit_observations;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.time.Instant;
import java.util.List;

/**
 * Represent a Waveform message. At this time, waveform data doesn't come with any direct identifiers for
 * the patient, only their location.
 * @author Jeremy Stein
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class WaveformMessage extends EmapOperationMessage {
    private String sourceObservationType = "waveform";

    private String locationString;

    /**
     * Sampling rate in Hz.
     */
    private int samplingRate;

    /**
     * Numeric value.
     */
    private InterchangeValue<List<Double>> numericValues = InterchangeValue.unknown();

    /**
     * Time of the observation.
     */
    private Instant observationTime;

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

}
