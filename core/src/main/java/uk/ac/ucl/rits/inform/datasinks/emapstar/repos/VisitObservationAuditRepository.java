package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;

/**
 * Visit Observation repository.
 * @author Stef Piatek
 */
public interface VisitObservationAuditRepository extends CrudRepository<VisitObservationAudit, Long> {

}
