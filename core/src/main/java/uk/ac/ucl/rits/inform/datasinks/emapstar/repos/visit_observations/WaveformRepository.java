package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.visit_observations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.Waveform;

/**
 * Waveform repository.
 *
 * @author Jeremy Stein
 */

public interface WaveformRepository extends CrudRepository<Waveform, Long> {
//    @Override
//    @QueryHints(value = { @QueryHint(name = "javax.persistence.query.timeout", value = "5000") })
//    spring.jpa.properties.hibernate.jdbc.batch_size = 500
//    <S extends Waveform> Iterable<S> saveAll(Iterable<S> entities);

    // XXX: add location to table
//    Iterable<Waveform> findAllByLocation(String location);
}
