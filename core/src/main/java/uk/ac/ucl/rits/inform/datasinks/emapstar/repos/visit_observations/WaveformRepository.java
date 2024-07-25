package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.Waveform;

import java.time.Instant;

/**
 * Waveform repository.
 *
 * @author Jeremy Stein
 */

public interface WaveformRepository extends CrudRepository<Waveform, Long> {
    Iterable<Waveform> findAllByLocationOrderByObservationDatetime(String location);

    // the default delete queries are very inefficient so specify manually
    @Modifying
    @Query("delete from Waveform where observationDatetime < :observationTime")
    int deleteAllInBatchByObservationDatetimeBefore(Instant observationTime);

    @Query("select max(w.observationDatetime) from Waveform w")
    Instant mostRecentObservationDatatime();
}
