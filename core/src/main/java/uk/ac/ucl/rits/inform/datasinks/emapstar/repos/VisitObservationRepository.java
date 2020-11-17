package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;

/**
 * Visit Observation repository.
 * @author Stef Piatek
 */
public interface VisitObservationRepository extends CrudRepository<VisitObservation, Long> {

}
