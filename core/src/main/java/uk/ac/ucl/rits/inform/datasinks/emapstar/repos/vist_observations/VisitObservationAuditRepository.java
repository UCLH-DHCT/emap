package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations;

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
     * @param visitId           hospital visit id
     * @param observationTypeId id of the visit observation type
     * @return optional visit observation audit
     */
    Optional<VisitObservationAudit> findByHospitalVisitIdAndVisitObservationTypeId(Long visitId, Long observationTypeId);
}
