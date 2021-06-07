package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationTypeAudit;

import java.util.Optional;

/**
 * Visit Observation Type Audit repository.
 * @author Stef Piatek
 */
public interface VisitObservationTypeAuditRepository extends CrudRepository<VisitObservationTypeAudit, Long> {

    /**
     * For testing.
     * @param idInApplication id in application
     * @param sourceSystem    source system
     * @return optional visit observation audit
     */
    Optional<VisitObservationAudit> findByIdInApplicationAndSourceSystem(String idInApplication, String sourceSystem);
}
