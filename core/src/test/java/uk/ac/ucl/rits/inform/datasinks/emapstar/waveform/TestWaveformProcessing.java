package uk.ac.ucl.rits.inform.datasinks.emapstar.waveform;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        long samplingRate;
        int maxSamplesPerMessage;
        String location;
        Instant obsDatetime;
        public Instant getEndObsTime() {
            return obsDatetime.plus(numSamples * 1000_000L / samplingRate, ChronoUnit.MICROS);
        }
        Long expectedLocationVisitId;
    }

    @Test
    @Sql("/populate_db.sql")
    void testAddWaveform() throws EmapOperationMessageProcessingException {
        var allTests = new TestData[]{
                // Intended to be two patients each connected to two machines, but the nature of the
                // bed/machine IDs may not quite be like this.
                new TestData(20_000, 300, 900,
                        "T11E^T11E BY02^BY02-25", Instant.parse("2010-09-10T12:00:00Z"), 106001L),
                new TestData(25_000, 50, 500,
                        "T42E^T42E BY03^BY03-17", Instant.parse("2010-09-14T15:27:00Z"), 106002L),
                new TestData(15_000, 300, 900,
                        // matches location but not time
                        "T11E^T11E BY02^BY02-25", Instant.parse("2010-09-14T16:00:00Z"), null),
                new TestData(17_000, 50, 500,
                        // matches time but not location
                        "T42E^T42E BY03^BY03-17", Instant.parse("2010-09-10T12:00:00Z"), null)
        };
        List<WaveformMessage> allMessages = new ArrayList<>();
        for (var test: allTests) {
            allMessages.addAll(
                    messageFactory.getWaveformMsgs(
                            test.samplingRate, test.numSamples, test.maxSamplesPerMessage, test.location, test.obsDatetime));
        }

        // must cope with messages in any order! Fixed seed to aid in debugging.
        Collections.shuffle(allMessages, new Random(42));

        for (WaveformMessage msg : allMessages) {
            processSingleMessage(msg);
        }

        int totalObservedNumSamples = 0;
        for (var test: allTests) {
            List<Waveform> waveformRows = filterByDatetimeInterval(
                    waveformRepository.findAllBySourceLocationOrderByObservationDatetime(test.location),
                    test.obsDatetime,
                    test.getEndObsTime());

            assertFalse(waveformRows.isEmpty());
            // make sure we're testing the difficult case of multiple waveform rows that need to be stitched together
            assertTrue(waveformRows.size() > 1);
            Optional<Integer> observedNumSamples = waveformRows.stream().map(w -> w.getValuesArray().length).reduce(Integer::sum);
            assertEquals(test.numSamples, observedNumSamples.orElseThrow());
            totalObservedNumSamples += observedNumSamples.get();
            /* If we expect the data to be linkable to an existing hospital visit,
             * search by that visit's (hl7adt) location and see that we get the same result.
             * If not, it should be empty.
             * Is this repo query even useful? Might want to add time interval to it.
             */
            List<Waveform> byHl7AdtLocation = filterByDatetimeInterval(
                    waveformRepository.findAllByLocationOrderByObservationDatetime(test.location),
                    test.obsDatetime,
                    test.getEndObsTime());

            if (test.expectedLocationVisitId == null) {
                assertTrue(waveformRows.stream().allMatch(aw -> aw.getLocationVisitId() == null));
                assertTrue(byHl7AdtLocation.isEmpty());
            } else {
                assertTrue(waveformRows.stream().allMatch(aw -> aw.getLocationVisitId().getLocationVisitId() == test.expectedLocationVisitId));

                // .equals does not work properly for our Entities, but we only need to check that it's
                // the same rows, so just check PKs
                Set<Long> idsByHl7Adt = byHl7AdtLocation.stream().map(Waveform::getWaveformId).collect(Collectors.toSet());
                Set<Long> idsBySource = waveformRows.stream().map(Waveform::getWaveformId).collect(Collectors.toSet());
                assertEquals(idsByHl7Adt, idsBySource);
            }
            List<Double> actualDataPointsAtLocation = new ArrayList<>();
            for (var row: waveformRows) {
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
            for (var row : waveformRows) {
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
            long totalActualTimeMicros = waveformRows.get(0).getObservationDatetime().until(projectedEndTime, ChronoUnit.MICROS);
            assertEquals(totalExpectedTimeMicros, totalActualTimeMicros);
        }
        assertEquals(Arrays.stream(allTests).map(d -> d.numSamples).reduce(Integer::sum).get(), totalObservedNumSamples);
    }

    private static List<Waveform> filterByDatetimeInterval(Iterable<Waveform> waveforms, Instant beginTime, Instant endTime) {
        List<Waveform> filteredRows = new ArrayList<>();
        for (var waveform: waveforms) {
            if (waveform.getObservationDatetime().compareTo(beginTime) >= 0
                    && waveform.getObservationDatetime().compareTo(endTime) < 0) {
                filteredRows.add(waveform);
            }
        }
        return filteredRows;
    }

}
