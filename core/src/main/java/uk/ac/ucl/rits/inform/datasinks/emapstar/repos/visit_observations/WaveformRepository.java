package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.Waveform;

/**
 * Waveform repository.
 *
 * @author Jeremy Stein
 */

public interface WaveformRepository extends CrudRepository<Waveform, Long> {
    Iterable<Waveform> findAllByLocationOrderByObservationDatetime(String location);
}
