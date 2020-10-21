package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;

import java.util.Optional;

/**
 * Visit Location repository.
 * @author Stef Piatek
 */
public interface LocationVisitRepository extends CrudRepository<LocationVisit, Integer> {
    /**
     * @param visit hospital visit
     * @return the LocationVisit wrapped in optional
     */
    Optional<LocationVisit> findByHospitalVisitIdAndDischargeTimeIsNull(HospitalVisit visit);


    /**
     * For testing: find by location string.
     * @param location full location string
     * @return LocationVisit wrapped in optional
     */
    Optional<LocationVisit> findByLocationIdLocationString(String location);

    /**
     * For testing: find location visit by the hospitalVisitId.
     * @param hospitalVisitId Id of the hospital visit
     * @return LocationVisit wrapped in optional
     */
    Optional<LocationVisit> findByDischargeTimeIsNullAndHospitalVisitIdHospitalVisitId(Long hospitalVisitId);
}
