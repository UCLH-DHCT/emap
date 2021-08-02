package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.BedFacility;
import uk.ac.ucl.rits.inform.informdb.movement.BedState;
import uk.ac.ucl.rits.inform.informdb.movement.Room;

import java.util.List;
import java.util.Optional;

/**
 * Bed Facility repository.
 * @author Stef Piatek
 */
public interface BedFacilityRepository extends CrudRepository<BedFacility, Long> {
    Optional<BedFacility> findByBedStateIdAndType(BedState bedStateId, String type);

    /**
     * For testing.
     * @param bedStateId bed state
     * @return all matching bed facilities.
     */
    List<BedFacility> findAllByBedStateIdOrderByType(BedState bedStateId);
}
