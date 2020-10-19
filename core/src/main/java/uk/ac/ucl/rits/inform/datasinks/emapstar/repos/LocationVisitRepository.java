package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;

import java.util.List;
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
    Optional<LocationVisit> findByHospitalVisitIdAndLocationVisitIdIsNull(HospitalVisit visit);


    /**
     * @param visit hospital visit
     * @return the LocationVisit wrapped in optional
     */
    List<LocationVisit> findAllByHospitalVisitId(HospitalVisit visit);
}
