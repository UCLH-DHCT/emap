package uk.ac.ucl.rits.inform.datasinks.emapstar.waveform;

import lombok.AllArgsConstructor;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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

    @AllArgsConstructor
    class TestData {
        int numSamples;
        int samplingRate;
        int maxSamplesPerMessage;
        String location;
    }

    @Test
    void testAddWaveform() throws EmapOperationMessageProcessingException {
        var allTests = new TestData[]{
                // Intended to be two patients each connected to two machines, but the nature of the
                // bed/machine IDs may not quite be like this.
                new TestData(20_000, 300, 900, "Location1MachineA"),
                new TestData(25_000, 50, 500, "Location1MachineB"),
                new TestData(15_000, 300, 900, "Location2MachineC"),
                new TestData(17_000, 50, 500, "Location2MachineD"),
        };
        List<WaveformMessage> allMessages = new ArrayList<>();
        for (var test: allTests) {
            allMessages.addAll(
                    messageFactory.getWaveformMsgs(
                            test.samplingRate, test.numSamples, test.maxSamplesPerMessage, test.location));
        }

        // must cope with messages in any order! Fixed seed to aid in debugging.
        Collections.shuffle(allMessages, new Random(42));

        for (WaveformMessage msg : allMessages) {
            processSingleMessage(msg);
        }

        for (var test: allTests) {
            List<Waveform> actualWaveformsAtLocation = new ArrayList<>();
            waveformRepository.findAllByLocationOrderByObservationDatetime(test.location).forEach(actualWaveformsAtLocation::add);
            // make sure we're testing the difficult case of multiple messages that need to be stitched together
            assertTrue(actualWaveformsAtLocation.size() > 1);
            Optional<Integer> observedNumSamples = actualWaveformsAtLocation.stream().map(w -> w.getValuesArray().length).reduce(Integer::sum);
            assertEquals(test.numSamples, observedNumSamples.orElseThrow());
            List<Double> actualDataPointsAtLocation = new ArrayList<>();
            for (var row : actualWaveformsAtLocation) {
                assertTrue(row.getValuesArray().length <= test.maxSamplesPerMessage);
                actualDataPointsAtLocation.addAll(Arrays.asList(row.getValuesArray()));
            }
            for (int i = 1; i < actualDataPointsAtLocation.size(); i++) {
                Double thisValue = actualDataPointsAtLocation.get(i);
                Double previousValue = actualDataPointsAtLocation.get(i - 1);
                // test data is a sine wave, check that it has plausible values
                assertTrue(-1 <= thisValue && thisValue <= 1);
                assertNotEquals(thisValue, previousValue);
            }
            Instant projectedEndTime = null;
            for (var row : actualWaveformsAtLocation) {
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
            long totalExpectedTimeMicros = 1_000_000L * test.numSamples / test.samplingRate;
            long totalActualTimeMicros = actualWaveformsAtLocation.get(0).getObservationDatetime().until(projectedEndTime, ChronoUnit.MICROS);
            assertEquals(totalExpectedTimeMicros, totalActualTimeMicros);
        }
    }

}
