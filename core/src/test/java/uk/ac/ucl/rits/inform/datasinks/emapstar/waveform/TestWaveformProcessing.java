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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
        int numSamples = 100_000;
        int samplingRate = 300;
        List<WaveformMessage> messages = messageFactory.getWaveformMsgs(samplingRate, numSamples);
        for (WaveformMessage msg : messages) {
            processSingleMessage(msg);
        }

        List<Waveform> allWaveforms = new ArrayList<>();
        waveformRepository.findAll().forEach(allWaveforms::add);
        assertEquals(numSamples, allWaveforms.size());
        // only the time *in between* samples, hence the (numSamples - 1)
        long totalExpectedTimeMillis = 1_000L * (numSamples - 1) / samplingRate;
        long expectedGapNanos = totalExpectedTimeMillis * 1_000_000L / numSamples;
        long totalActualTimeNanos = 0;
        for (int i = 1; i < numSamples; i++) {
            var previousTimeStamp = allWaveforms.get(i - 1).getObservationDatetime();
            var thisTimeStamp = allWaveforms.get(i).getObservationDatetime();
            long nanosBetween = previousTimeStamp.until(thisTimeStamp, ChronoUnit.NANOS);
            totalActualTimeNanos += nanosBetween;
            // The temporal resolution of the underlying database, and the rounding behaviour of Instant.until
            // introduces some variability here.
            assertTrue(nanosBetween >= expectedGapNanos * 0.999);
            assertTrue(nanosBetween <= expectedGapNanos * 1.001);
            Double thisValue = allWaveforms.get(i).getValueAsReal();
            Double previousValue = allWaveforms.get(i - 1).getValueAsReal();
            // test data is a sine wave, check that it has plausible values
            assertTrue(-1 <= thisValue && thisValue <= 1);
            assertNotEquals(thisValue, previousValue);
        }
        assertEquals(totalExpectedTimeMillis, totalActualTimeNanos / 1_000_000);
    }

}
