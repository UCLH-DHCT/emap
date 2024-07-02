package uk.ac.ucl.rits.inform.datasinks.emapstar.waveform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations.VisitObservationAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations.VisitObservationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations.WaveformRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.Waveform;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestWaveformProcessing extends MessageProcessingBase {
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private WaveformRepository waveformRepository;
    @Autowired
    private VisitObservationRepository visitObservationRepository;
    @Autowired
    private VisitObservationAuditRepository visitObservationAuditRepository;
    @Autowired
    private VisitObservationTypeRepository visitObservationTypeRepository;

    @BeforeEach
    void setup() throws IOException {
    }

    @Test
    void testAddWaveform() throws EmapOperationMessageProcessingException {
        int numSamples = 20_000;
        int samplingRate = 300;
        List<WaveformMessage> messages = messageFactory.getWaveformMsgs(
                samplingRate, numSamples, samplingRate * 3, "LOCATION1");

        for (WaveformMessage msg : messages) {
            processSingleMessage(msg);
        }

        List<Waveform> allWaveforms = new ArrayList<>();
//        waveformRepository.findAllByLocation("LOCATION1").forEach(allWaveforms::add);
        waveformRepository.findAll().forEach(allWaveforms::add);
        assertTrue(allWaveforms.size() > 1); // make sure we're testing the difficult case
        Optional<Integer> observedNumSamples = allWaveforms.stream().map(w -> w.getValuesArray().length).reduce(Integer::sum);
        assertEquals(numSamples, observedNumSamples.orElseThrow());
        List<Double> allDataPoints = new ArrayList<>();
        for (var row: allWaveforms) {
            allDataPoints.addAll(Arrays.asList(row.getValuesArray()));
        }
        for (int i = 1; i < allDataPoints.size(); i++) {
            Double thisValue = allDataPoints.get(i);
            Double previousValue = allDataPoints.get(i - 1);
            // test data is a sine wave, check that it has plausible values
            assertTrue(-1 <= thisValue && thisValue <= 1);
            assertNotEquals(thisValue, previousValue);
        }
        Instant projectedEndTime = null;
        for (var row: allWaveforms) {
            Instant thisStartTime = row.getObservationDatetime();
            if (projectedEndTime != null) {
                // rows should neatly abut
                assertEquals(thisStartTime, projectedEndTime);
            }
            // the final point in the array nominally becomes invalid (1 / samplingRate)
            // seconds after its start time
            projectedEndTime = thisStartTime.plus(
                    row.getValuesArray().length * 1000_000 / row.getSamplingRate(),
                    ChronoUnit.MICROS);
        }
        long totalExpectedTimeMicros = 1_000_000L * numSamples / samplingRate;
        long totalActualTimeMicros = allWaveforms.get(0).getObservationDatetime().until(projectedEndTime, ChronoUnit.MICROS);
        assertEquals(totalExpectedTimeMicros, totalActualTimeMicros);
    }

}
