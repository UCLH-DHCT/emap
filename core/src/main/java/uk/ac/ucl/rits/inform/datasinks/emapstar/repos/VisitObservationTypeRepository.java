package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;

/**
 * Visit Observation repository.
 * @author Stef Piatek
 */
public interface VisitObservationTypeRepository extends CrudRepository<VisitObservationType, Long> {

}
