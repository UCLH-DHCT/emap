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
        // xXX: need to supply multiple messages here
        List<WaveformMessage> messages = messageFactory.getWaveformMsgs(samplingRate, numSamples, "LOCATION1");
        for (WaveformMessage msg : messages) {
            processSingleMessage(msg);
        }

        List<Waveform> allWaveforms = new ArrayList<>();
//        waveformRepository.findAllByLocation("LOCATION1").forEach(allWaveforms::add);
        waveformRepository.findAll().forEach(allWaveforms::add);
        assertTrue(allWaveforms.size() > 1); // make sure we're testing the difficult case
        Optional<Integer> observedNumSamples = allWaveforms.stream().map(w -> w.getValuesArray().length).reduce(Integer::sum);
        assertEquals(numSamples, observedNumSamples.orElseThrow());
        // only the time *in between* samples, hence the (numSamples - 1)
        long totalExpectedTimeMillis = 1_000L * (numSamples - 1) / samplingRate;
        long expectedGapNanos = totalExpectedTimeMillis * 1_000_000L / numSamples;
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
            projectedEndTime = thisStartTime.plus(
                    row.getValuesArray().length * 1000_000 / row.getSamplingRate(),
                    ChronoUnit.MICROS);
        }
        long totalActualTimeMicros = allWaveforms.get(0).getObservationDatetime().until(projectedEndTime, ChronoUnit.MICROS);
        assertEquals(totalExpectedTimeMillis, totalActualTimeMicros / 1_000);
    }

}
