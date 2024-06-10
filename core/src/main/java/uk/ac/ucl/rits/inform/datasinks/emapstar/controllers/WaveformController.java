package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public void processWaveform(WaveformMessage msg, Instant storedFrom) {
        InterchangeValue<List<Double>> numericValue = msg.getNumericValue();
        if (! numericValue.isSave())
            return;
        // location goes in some other table...
//        msg.getLocationString();
        for (Double val: numericValue.get()) {
            Waveform waveform = new Waveform(
//                    visitObservationTypeId,
                    msg.getObservationTime(),
                    msg.getObservationTime(),
                    storedFrom);
            waveform.setValueAsReal(val);
            waveformRepository.save(waveform);
        }
    }

}
