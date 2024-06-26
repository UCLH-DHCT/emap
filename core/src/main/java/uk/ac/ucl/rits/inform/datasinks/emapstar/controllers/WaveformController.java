package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations.WaveformRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.Waveform;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.util.List;

/**
 * Controller for LabResult specific information.
 * @author Stef Piatek
 */
@Component
public class WaveformController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WaveformRepository waveformRepository;

    WaveformController(
            WaveformRepository waveformRepository
    ) {
        this.waveformRepository = waveformRepository;
    }

    /**
     * Process waveform data message.
     * @param msg the interchange message
     * @param storedFrom stored from timestamp
     * @throws MessageIgnoredException if message not processed
     */
    @Transactional
    public void processWaveform(WaveformMessage msg, Instant storedFrom) throws MessageIgnoredException {
        InterchangeValue<List<Double>> interchangeValue = msg.getNumericValues();
        long samplingRate = msg.getSamplingRate();
        if (!interchangeValue.isSave()) {
            throw new MessageIgnoredException("Updating/deleting waveform data is not supported");
        }
        List<Double> numericValues = interchangeValue.get();
        Instant observationTime = msg.getObservationTime();
        for (int i = 0; i < numericValues.size(); i++) {
            Double val = numericValues.get(i);
            Instant impliedTime = observationTime.plusNanos(i * 1000_000_000L / samplingRate);
            Waveform waveform = new Waveform(
                    impliedTime,
                    impliedTime,
                    storedFrom);
            waveform.setValueAsReal(val);
            waveformRepository.save(waveform);
        }
    }

}
