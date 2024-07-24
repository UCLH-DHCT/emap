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
 * Controller for Waveform specific information.
 * @author Jeremy Stein
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
        if (!interchangeValue.isSave()) {
            throw new MessageIgnoredException("Updating/deleting waveform data is not supported");
        }
        // All given values are put into one new row. It's the responsibility of whoever is
        // generating the message to chose an appropriate size of array.
        List<Double> numericValues = interchangeValue.get();
        Instant observationTime = msg.getObservationTime();
        Waveform dataPoint = new Waveform(
                observationTime,
                observationTime,
                storedFrom);
        Double[] valuesAsArray = numericValues.toArray(new Double[0]);
        dataPoint.setSamplingRate(msg.getSamplingRate());
        dataPoint.setLocation(msg.getLocationString());
        dataPoint.setValuesArray(valuesAsArray);
        waveformRepository.save(dataPoint);
    }
}
