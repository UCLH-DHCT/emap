package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Visit Observation repository.
 *
 * @author Stef Piatek
 */
public interface VisitObservationRepository extends CrudRepository<VisitObservation, Long> {

    /**
     * @param visit           hospital visit
     * @param type            visit observation type
     * @param observationTime initial time of the observation
     * @return optional visit observation
     */
    Optional<VisitObservation> findByHospitalVisitIdAndVisitObservationTypeIdAndObservationDatetime(
            HospitalVisit visit, VisitObservationType type, Instant observationTime);

    /**
     * For testing.
     *
     * @param visit hospital visit
     * @return list of visit observations
     */
    List<VisitObservation> findAllByHospitalVisitId(HospitalVisit visit);

    /**
     * For testing.
     *
     * @param visit       hospital visit
     * @param interfaceId Identifier in interface
     * @return optional visit observation
     */
    Optional<VisitObservation> findByHospitalVisitIdAndVisitObservationTypeIdInterfaceId(
            HospitalVisit visit, String interfaceId);

    /**
     * For testing.
     *
     * @param visit           hospital visit
     * @param idInApplication Identifier in application
     * @return optional visit observation
     */
    Optional<VisitObservation> findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(
            HospitalVisit visit, String idInApplication);
}
