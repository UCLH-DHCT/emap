package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Bed;
import uk.ac.ucl.rits.inform.informdb.movement.BedState;

import java.util.List;
import java.util.Optional;

/**
 * Bed repository.
 * @author Stef Piatek
 */
public interface BedStateRepository extends CrudRepository<BedState, Long> {
    List<BedState> findAllByBedIdOrderByValidFromDesc(Bed bed);

    /**
     * For testing.
     * @param csn CSN to find
     * @return potential bed state
     */
    Optional<BedState> findByCsn(long csn);

}
