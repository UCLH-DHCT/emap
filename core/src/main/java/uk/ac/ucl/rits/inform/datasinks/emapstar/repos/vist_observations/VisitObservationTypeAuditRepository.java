package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationTypeAudit;


/**
 * Visit Observation Type Audit repository.
 * @author Stef Piatek
 */
public interface VisitObservationTypeAuditRepository extends CrudRepository<VisitObservationTypeAudit, Long> {

}
