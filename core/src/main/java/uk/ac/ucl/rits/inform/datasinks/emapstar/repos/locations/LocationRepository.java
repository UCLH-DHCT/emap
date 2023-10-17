package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Location;

import java.util.Optional;

/**
 * Location repository.
 * @author Stef Piatek
 */
public interface LocationRepository extends CrudRepository<Location, Long> {
    /**
     * @param locationString Location string
     * @return the Location wrapped in optional
     */
    Optional<Location> findByLocationStringEquals(String locationString);

    /**
     * @param locationString Location string
     * @return true if location exists
     */
    boolean existsByLocationStringEquals(String locationString);
}
