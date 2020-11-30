package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;

import java.util.Optional;

/**
 * Visit Observation repository.
 * @author Stef Piatek
 */
public interface VisitObservationAuditRepository extends CrudRepository<VisitObservationAudit, Long> {

    /**
     * For testing.
     * @param visitId         hospital visit id
     * @param idInApplication id in application
     * @return optional visit observation audit
     */
    Optional<VisitObservationAudit> findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(Long visitId, String idInApplication);
}
